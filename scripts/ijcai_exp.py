import argparse
from functools import wraps
import logging
import logging.config
import multiprocessing
import os
import pathlib

SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / 'rlexperiment.sh'
CONFIG_PATH = pathlib.Path(__file__).resolve().parents[1] / 'ijcai-experiments'

DEFAULT_EXPERIMENTS = ['specific', 'nemesis']
DEFAULT_ADVERSARIES = [
    'AHTN',
    'NaiveMCTS',
    'PuppetAB',
    'PuppetMCTS',
    'StrategyTactics'
]
DEFAULT_ITERATIONS = 5
DEFAULT_OUTPUT = pathlib.Path(__file__).resolve().parents[1] / 'experiment-results'

def async_function(function):
    """Decorator for async functions."""
    @wraps(function)
    def wrapper(*args, **kwargs):
        process = multiprocessing.Process(
            target=function, args=args, kwargs=kwargs)
        process.start()
        return process
    return wrapper


def create_dirs(path):
    try:
        os.makedirs(path)
    except FileExistsError:
        # directory already exists
        return


# TODO: improve function
def generate_call(script, p1_seed=None, p2_seed=None,
                  p1_bin_in=None, p2_bin_in=None,
                  p1_bin_out=None, p2_bin_out=None,
                  p1_dir=None, p2_dir=None,
                  config=None, output_path=None, log_path=None):
    call = [f'{script}']
    if config is not None:
        call.append(f'-c {config}')
    if p1_seed is not None:
        call.append(f'-s1 {p1_seed}')
    if p2_seed is not None:
        call.append(f'-s2 {p2_seed}')
    if p1_bin_in:
        call.append(f'-bi1 {p1_bin_in}')
    if p2_bin_in:
        call.append(f'-bi2 {p2_bin_in}')
    if p1_bin_out:
        call.append('-b1')
    if p2_bin_out:
        call.append('-b2')
    if p1_dir is not None:
        call.append(f'-d1 {p1_dir}')
    if p2_dir is not None:
        call.append(f'-d2 {p2_dir}')
    if output_path is not None:
        call.append(f'-o {output_path}')
    if log_path is not None:
        call.append(f'> {log_path}')
    return ' '.join(call)


@async_function
def specific_experiment(adversary, seed, output):
    logger = logging.getLogger(f'Specific - {adversary} {seed}')

    working_dir = f'{output}/specific/{adversary}/rep_{seed}'
    create_dirs(working_dir)

    # Training
    config = f'{CONFIG_PATH}/specific/{adversary}/train.properties'
    p1_dir = f'{working_dir}'
    output_path = f'{p1_dir}/train-results.txt'
    log = f'{p1_dir}/train.log'

    logger.info("Starting training.")
    os_train_call = generate_call(
        SCRIPT_PATH,
        config=config,
        p1_seed=seed,
        p1_bin_out=True,
        p1_dir=p1_dir,
        output_path=output_path,
        log_path=log)
    os.system(os_train_call)

    # Testing
    config = f'{CONFIG_PATH}/specific/{adversary}/test.properties'
    p1_bin_in = f'{p1_dir}/weights_0.bin'
    output_path = f'{p1_dir}/test-results.txt'
    log = f'{p1_dir}/test.log'

    logger.info("Starting testing.")
    os_test_call = generate_call(
        SCRIPT_PATH,
        config=config,
        p1_seed=seed,
        p1_bin_in=p1_bin_in,
        output_path=output_path,
        log_path=log)
    os.system(os_test_call)

    logger.info('Experiment done.')


@async_function
def nemesis_experiment(adversary, seed, output):
    logger = logging.getLogger(f'Nemesis - {adversary} {seed}')

    working_dir = f'{output}/nemesis/{adversary}/rep_{seed}'
    create_dirs(working_dir)

    # PuppetMCTS training
    config = f'{CONFIG_PATH}/nemesis/PuppetMCTS/train.properties'
    p1_dir = f'{working_dir}'
    output_path = f'{p1_dir}/puppet-train-results.txt'
    log = f'{p1_dir}/puppet-train.log'

    logger.info("Starting training phase 1.")
    os_train_call = generate_call(
        SCRIPT_PATH,
        config=config,
        p1_seed=seed,
        p1_bin_out=True,
        p1_dir=p1_dir,
        output_path=output_path,
        log_path=log)
    os.system(os_train_call)

    # Selfplay training
    config = f'{CONFIG_PATH}/nemesis/Metabot/train.properties'
    p2_bin_in = f'{p1_dir}/weights_0.bin'
    output_path = f'{p1_dir}/metabot-train-results.txt'
    log = f'{p1_dir}/metabot-train.log'

    logger.info("Starting training phase 2.")
    os_train_call = generate_call(
        SCRIPT_PATH,
        config=config,
        p1_seed=seed,
        p2_seed=seed,
        p2_bin_in=p2_bin_in,
        p1_bin_out=True,
        p1_dir=p1_dir,
        output_path=output_path,
        log_path=log)
    os.system(os_train_call)

    # Testing
    config = f'{CONFIG_PATH}/nemesis/{adversary}/test.properties'
    p1_bin_in = f'{p1_dir}/weights_0.bin'
    output_path = f'{p1_dir}/test-results.txt'
    log = f'{p1_dir}/test.log'

    logger.info("Starting testing.")
    os_test_call = generate_call(
        SCRIPT_PATH,
        config=config,
        p1_seed=seed,
        p1_bin_in=p1_bin_in,
        output_path=output_path,
        log_path=log)
    os.system(os_test_call)

    logger.info('Experiment done.')


@async_function
def adversary_thread(experiment, adversary, iterations, output):
    # TODO: Add logging messages

    threads = []

    for i in range(iterations):
        thread = experiment(adversary, i, output)
        threads.append(thread)

    for thread in threads:
        thread.join()


@async_function
def multithreading_experiments(experiment, adversaries, iterations, output):
    # TODO: Add logging messages

    threads = []

    for adv in adversaries:
        thread = adversary_thread(experiment, adv, iterations, output)
        threads.append(thread)

    for thread in threads:
        thread.join()


def ijcai_experiments(experiments, adversaries, iterations, output):
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s - %(levelname)s - %(name)s - %(message)s')
    logger = logging.getLogger('IJCAI Experiments')

    # Check if selected experiments are valid
    if not experiments:
        logger.error('No experiments selected!')
        return
    if not set(experiments).issubset(DEFAULT_EXPERIMENTS):
        logger.error('Unsuported experiment')
        return

    # Check if selected adversaries are valid
    if not adversaries:
        logger.error('No adversaries selected!')
        return
    if not set(adversaries).issubset(DEFAULT_ADVERSARIES):
        logger.error('Unsuported adversary')
        return

    # Check if number of iterations is valid
    if iterations <= 0:
        logger.error("Invalid int value: %d", iterations)
        return

    threads = []

    if 'specific' in experiments:
        logger.info("Launching specific experiments.")
        thread = multithreading_experiments(specific_experiment, adversaries, iterations, output)
        threads.append(thread)

    if 'nemesis' in experiments:
        logger.info("Launching nemesis experiments.")
        thread = multithreading_experiments(nemesis_experiment, adversaries, iterations, output)
        threads.append(thread)

    for thread in threads:
        thread.join()

    logger.info('IJCAI experiments done.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-e', '--experiments', nargs='*', default=DEFAULT_EXPERIMENTS)
    parser.add_argument('-a', '--adversaries', nargs='*', default=DEFAULT_ADVERSARIES)
    parser.add_argument('-i', '--iterations', type=int, default=5)
    parser.add_argument('-o', '--output', default=DEFAULT_OUTPUT)
    cmd_args = parser.parse_args()

    ijcai_experiments(
        cmd_args.experiments,
        cmd_args.adversaries,
        cmd_args.iterations,
        cmd_args.output)
