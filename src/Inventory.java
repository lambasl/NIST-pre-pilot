
public class Inventory {
	
	int lane_id;
	int zone_id;
	int lane_number;
	String road;
	String direction;
	String location_description;
	String latitude;
	String longitutde;
	int default_speed;
	int interval;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(lane_id).append("|").append(zone_id).append("|").append(lane_number).append("|").append(road).append("|").append(direction)
		.append("|").append(location_description).append("|").append(latitude).append("|").append(longitutde).append("|").append(default_speed).append("|").append(interval);
		return sb.toString();
	}
}
