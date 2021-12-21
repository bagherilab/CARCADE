package arcade.agent.cell;

import java.util.Map;
import arcade.sim.Simulation;
import arcade.util.Parameter;
import arcade.agent.module.*;
import arcade.env.loc.Location;

/** 
 * Extension of {@link arcade.agent.cell.CARTCell} for CD4 CART-cells with
 * selected module versions.
 */

public class CART4Cell extends CARTCell {
	/** Serialization version identifier */
	private static final long serialVersionUID = 0;
	
	/**
	 * Creates a CD4 {@link arcade.agent.cell.CARTCell} agent.
	 * 
	 * @param sim  the simulation instance
	 * @param pop  the population index
	 * @param loc  the location of the cell 
	 * @param vol  the initial (and critical) volume of the cell
	 * @param age  the initial age of the cell in minutes
	 * @param params  the map of parameter name to {@link arcade.util.Parameter} objects
	 */
	public CART4Cell(Simulation sim, int pop, Location loc, double vol, int age, Map<String, Parameter> params) {
		super(pop, loc, vol, age, params);
		this.tcode = CODE_4_CELL;
		modules.put("inflammation", new InflammationCD4(this, sim));
		modules.put("metabolism",  new MetabolismCART(this, sim));
	}
	
	public Cell newCell(Simulation sim, int pop, Location loc, double vol, Map<String, Parameter> params) {
		return new CART4Cell(sim, pop, loc, vol, 0, params);
	}
}

