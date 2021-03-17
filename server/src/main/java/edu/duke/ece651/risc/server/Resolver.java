package edu.duke.ece651.risc.server;

import edu.duke.ece651.risc.shared.Board;
import edu.duke.ece651.risc.shared.Territory;
public interface Resolver<T> {
  public void combineEnemyArmy(Board<T> board);

  public void executeAllBattle(Board<T> board);

  public void combatOnTerritory(Territory<T> battleField);
}