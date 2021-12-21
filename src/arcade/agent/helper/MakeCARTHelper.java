package arcade.agent.helper;

import sim.engine.*;
import arcade.sim.Simulation;
import arcade.agent.cell.Cell;
import arcade.agent.cell.CARTCell;
import arcade.env.loc.Location;

/** 
 * Extension of {@link arcade.agent.helper.CARTHelper} for cell division.
 * Adapted from {@link arcade.agent.helper.MakeTissueHelper}.
 * <p>
 * {@code MakeCARTHelper} is repeatedly stepped from its creation until either
 * the cell is no longer able to proliferate or it has successfully doubled in
 * size and is able to create a new cell object.
 */

public class MakeCARTHelper extends CARTHelper {
	/** Serialization version identifier */
	private static final long serialVersionUID = 0;
	
	/** Stopper used to stop this helper from being stepped in the schedule */
	private Stoppable stopper;
	
	/** Time required for DNA synthesis (in minutes */
	private double synthTime;
	
	/** Tentative new daughter cell agent */
	private final Cell cNew;
	
	/** Tracker for duration of cell cycle */
	private int ticker;
	
	/** Time at beginning of cell cycle */
	private final double start;
	
	/** Volume fraction for daughter cell */
	private final double f;
	
	
	/**
	 * Creates a {@code MakeCARTHelper} for the given
	 * {@link arcade.agent.cell.CARTCell}.
	 * 
	 * @param c  the {@link arcade.agent.cell.CARTCell} the helper is associated with
	 * @param cNew  the new {@link arcade.agent.cell.CARTCell} to be added
	 * @param start  the tick at which the helper is created
	 * @param f  the volume fraction for the daughter cell (between 0.45 and 0.55)
	 */
	public MakeCARTHelper(Cell c, Cell cNew, double start, double f) {
		super((CARTCell)c);
		ticker = 0;
		this.start = start;
		this.f = f;
		this.cNew = cNew;
	}
	
	public void scheduleHelper(Simulation sim) { scheduleHelper(sim, sim.getTime()); }
	
	/**
	 *{@inheritDoc}
	 * <p>
	 * {@code MakeCARTHelper} is scheduled repeating until division is complete.
	 */
	public void scheduleHelper(Simulation sim, double begin) {
		double synthMean = sim.getSeries().getParam(c.getPop(), "SYNTHESIS_TIME_T");
		double synthRange = sim.getSeries().getParam(c.getPop(), "SYNTHESIS_RANGE_T");
		synthTime = synthMean + Math.round(synthRange*(2*sim.getRandom() - 1));
		this.begin = sim.getTime();
		stopper = ((SimState)sim).schedule.scheduleRepeating(this.begin + 1, Simulation.ORDERING_HELPER, this);
	}
	
	/**
	 * Stops the helper from stepping before division is complete.
	 * 
	 * @param sim  the simulation instance
	 * @param pause  {@code true} if cell becomes paused, {@code false} otherwise
	 */
	private void stop(Simulation sim, boolean pause) {
		end = sim.getTime();
		c.setFlag(Cell.IS_PROLIFERATING, false);
		c.helper = null;
		stopper.stop();
		if (pause) { c.pause(sim); }
	}
	
	public void stop() {
		c.setFlag(Cell.IS_PROLIFERATING, false);
		if (c.helper == this) { c.helper = null; }
		stopper.stop();
	}
	
	/**
	 * Steps the helper for making a cell.
	 * 
	 * @param state  the MASON simulation state
	 */
	public void step(SimState state) {
		Simulation sim = (Simulation)state;
		if (c.isStopped()) { stop(sim, false); return; }
		
		// Check if cell is no longer able to proliferate due to (i) other
		// condition that has caused its type to no longer be proliferative,
		// or (ii) no space in neighborhood to divide into. Otherwise, 
		// check if double volume has been reached, and if so, create a new cell.
		if (c.getType() != Cell.TYPE_PROLI) { stop(sim, false); }
		else {
			Location newLoc = CARTCell.getBestLocation(sim, cNew);
			
			if (newLoc == null) { stop(sim, true); }
			else if (c.getFlag(Cell.IS_DOUBLED)) {
				if (ticker > synthTime) {
					// Turn off doubled flag.
					c.setFlag(Cell.IS_DOUBLED, false);
					
					// Add cycle time to tracker.
					c.addCycle(sim.getTime() - start);
					
					// Set location of new cell and add to schedule.
					cNew.getLocation().updateLocation(newLoc);
					sim.getAgents().addObject(cNew, cNew.getLocation());
					cNew.setStopper(((SimState)sim).schedule.scheduleRepeating(cNew, Simulation.ORDERING_CELLS, 1));
					
					// Update daughter cell modules.
					cNew.getModule("inflammation").updateModule(c.getModule("inflammation"), f);
					cNew.getModule("metabolism").updateModule(c.getModule("metabolism"), f);
					
					// Update environment generator components.
					sim.getEnvironment("sites").getComponent("sites").updateComponent(sim, c.getLocation(), newLoc);
					
					// Update number of divisions for parent and daughter cell
					// Set daughter self receptors and activated flag to match parent.
					// Set parent type back to neutral.
					c.divisions--;
					((CARTCell)cNew).divisions = ((CARTCell)c).divisions;
					((CARTCell)cNew).selfReceptors = c.selfReceptors;
					((CARTCell)cNew).boundAntigenCount = c.boundAntigenCount;
					((CARTCell)cNew).boundSelfCount = c.boundSelfCount;
					((CARTCell)cNew).setFlag(Cell.IS_ACTIVATED, c.getFlag(Cell.IS_ACTIVATED));
					c.setType(Cell.TYPE_NEUTRAL);
					c.setFlag(Cell.IS_PROLIFERATING, false);
					if (c.helper == this) { c.helper = null; }
					end = sim.getTime();
					stopper.stop();
				} else { ticker++; }
			}
		}
	}
}
