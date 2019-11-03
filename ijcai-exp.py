import argparse
import logging
import logging.config
import os
import threading

SCRIPT = 'rlexperiment.sh'
SPECIFIC_CONFIG_PATH = 'ijcai-experiments/specific'
NEMESIS_CONFIG_PATH = 'ijcai-experiments/nemesis'

ADVERSARIES = [
    'AHTN',
    'NaiveMCTS',
    'PuppetAB',
    'PuppetMCTS',
    'StrategyTactics'
]

def create_directories(path, n):
    # Create main directory
    if not os.path.isdir(path):
        try:
            os.makedirs(path)
        except OSError as e:
            logging.info(f'Creation of directory {path} failed!')

    # Create one directory for each repetition
    for i in range(n):
        dir_name = f'{path}/rep_{i}'
        if not os.path.isdir(dir_name):
            try:
                os.mkdir(dir_name)
            except OSError as e:
                logging.info(f'Creation of directory {dir_name} failed!')


def train(config, seed, path, working_dir, p2_seed=None, p2_bin_input=None):
    if p2_bin_input == None:
        os.system(f'./{SCRIPT} -b1 -s1 {seed} -d1 {path}/{working_dir} -c {config} -o {path}/{working_dir}/train-results.txt > {path}/{working_dir}/train.log')
    else:
        os.system(f'./{SCRIPT} -b1 -s1 {seed} -s2 {p2_seed} -d1 {path}/{working_dir} -c {config} -bi2 {p2_bin_input} -o {path}/{working_dir}/train-results.txt > {path}/{working_dir}/train.log')


def test(config, seed, path, working_dir, bin_input):
    os.system(f'./{SCRIPT} -s1 {seed} -d1 {path}/{working_dir} -c {config} -bi1 {bin_input} -o {path}/{working_dir}/test-results.txt > {path}/{working_dir}/test.log')


def train_and_test(n, name, train_config, test_config, path):
    logger = logging.getLogger(f'{name}')

    create_directories(path, n)

    for i in range(n):
        logger.info(f'Launching repetition {i} train.')
        train(train_config, i, path, f'rep_{i}')
        logger.info(f'Launching repetition {i} test.')
        test(test_config, i, path, f'rep_{i}', f'{path}/rep_{i}/weights_0.bin')

    logger.info('Done.')


def specific_experiments(n, directory):
    logger = logging.getLogger('Specific experiments')

    logger.info("Launching specific experiments.")

    exp_threads = []

    for adv in ADVERSARIES:
        t = threading.Thread(
                target=train_and_test,
                args=(
                    n,
                    adv,
                    f'{SPECIFIC_CONFIG_PATH}/{adv}/train.properties',
                    f'{SPECIFIC_CONFIG_PATH}/{adv}/test.properties',
                    f'{directory}/{adv}'
                ))
        exp_threads.append(t)

    for thread in exp_threads:
        thread.start()

    for thread in exp_threads:
        thread.join()

    logger.info('Done.')


def nemesis_exec(i, directory):
    logger = logging.getLogger(f'Repetition {i}')

    logger.info(f'Launching PuppetMCTS train.')
    train(f'{NEMESIS_CONFIG_PATH}/PuppetMCTS/train.properties', i, f'{directory}/PuppetMCTS', f'rep_{i}')

    # Selfplay training
    logger.info(f'Launching selfplay train.')
    train(f'{NEMESIS_CONFIG_PATH}/Metabot/train.properties', i, f'{directory}/Metabot', f'rep_{i}', i, f'{directory}/PuppetMCTS/rep_{i}/weights_0.bin')

    for adv in ADVERSARIES:
        logger.info(f'Lauching {adv} test.')
        test(f'{NEMESIS_CONFIG_PATH}/{adv}/test.properties',
             i,
             f'{directory}/{adv}',
             f'rep_{i}',
             f'{directory}/Metabot/rep_{i}/weights_0.bin')

    logger.info('Done.')


def nemesis_experiments(n, directory):
    logger = logging.getLogger('Nemesis experiments')

    logger.info("Launching nemesis experiments.")
    
    create_directories(f'{directory}/Metabot', n)
    for adv in ADVERSARIES:
        create_directories(f'{directory}/{adv}', n)

    exp_threads = []

    for i in range(n):
        exp_threads.append(threading.Thread(target=nemesis_exec, args=(i, directory)))

    for thread in exp_threads:
        thread.start()

    for thread in exp_threads:
        thread.join()

    logger.info('Done.')


def main(args):
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s - %(levelname)s - %(name)s - %(message)s')
    logger = logging.getLogger('Main')
    logger.info("Launching IJCAI experiments.")

    if args.specific or not args.nemesis:
        specific_experiments(5, 'experiments/specific')
    
    if args.nemesis or not args.specific:
        nemesis_experiments(5, 'experiments/nemesis')

    logger.info('IJCAI experiments done.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--specific', action='store_true', required=False)
    parser.add_argument('--nemesis', action='store_true', required=False)
    args = parser.parse_args()
    main(args)
