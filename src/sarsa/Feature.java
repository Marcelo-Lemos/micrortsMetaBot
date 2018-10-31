package sarsa;

public class Feature {
	public float max;
	public float min;
	public float value;
	public String name;
	
	public Feature(String name, float value, float min, float max){
		this.name = name;
		this.value = value;
		this.max = max;
		this.min = min;
	}
}
