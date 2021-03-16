package edu.duke.ece651.risc.shared;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MoveOrderConsistencyCheckerTest {
  @Test
  public void test_checkMyRule() {

    BoardFactory<String> f = new V1BoardFactory<>();
    Board<String> b = f.makeGameBoard(2);
    assertEquals(b.occupyTerritory(0, 0), false); // group 0 owner is 0
    assertEquals(b.occupyTerritory(1, 1), false); // group 1 owner is 1

    MoveOrder<String> m1 = new MoveOrder<>("0 0 0");
    MoveOrder<String> m2 = new MoveOrder<>("0 1 2");

    MoveOrder<String> m3 = new MoveOrder<String>("3 3 0");
    MoveOrder<String> m4 = new MoveOrder<String>("3 4 2");

    
  }

}
