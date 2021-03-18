package edu.duke.ece651.risc.server;

import edu.duke.ece651.risc.shared.*;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import java.io.IOException;
import java.lang.Thread;

public class GameHostThread<T> extends Thread {

  private PlayerEntity<T> player;
  private int remainedUnits;
  private Board<T> board;
  private BoardView<T> view;
  private OrderRuleChecker<T> moveChecker;
  private OrderRuleChecker<T> attackChecker;
  private CyclicBarrier barrier;

  public GameHostThread(PlayerEntity<T> player, int units, Board<T> board, BoardView<T> view,
      OrderRuleChecker<T> moveChecker, OrderRuleChecker<T> attackChecker, CyclicBarrier barrier) {
    this.player = player;
    this.remainedUnits = units;
    this.board = board;
    this.view = view;
    this.moveChecker = moveChecker;
    this.attackChecker = attackChecker;
    this.barrier = barrier;
  }

  public void pickTerritory() throws IOException, ClassNotFoundException {
    // player side : receive game map, send choice, wait for choice info
    // success break, otherwise, go back

    // server side : send displayboard, receive choice, use occupyterritory,
    // if true, send success
    // if false, send info, and then send showgroup again
    player.sendObject(view.displayFullBoard());
    while (true) {
      String choice = (String) player.receiveObject();
      // TODO: occupy should be synchronized
      boolean occupied = board.occupyTerritory(Integer.parseInt(choice), player.getPlayerId());
      if (!occupied) {
        player.sendObject(Constant.VALID_MAP_CHOICE_INFO);
        break;
      } else {
        player.sendObject(Constant.INVALID_MAP_CHOICE_INFO);
      }
    }
    board.updateAllPrevDefender();
  }

  public void deployUnits() throws IOException, ClassNotFoundException {
    // player side: receive msg, send deployment (arraylist), receive info
    // continue, until finish deploy

    // servier side: send msg (display group), receive deployment (arraylist), send
    // info (if all done, send finish deploy)
    while (remainedUnits > 0) {
      String msg = view.displayGroup(player.getPlayerId()) + "You have " + remainedUnits + " left.";
      player.sendObject(msg);
      ArrayList<Integer> deployment = (ArrayList<Integer>) player.receiveObject();
      int territoryId = deployment.get(0);
      int unitAmount = deployment.get(1);
      // check units
      // >0 continue
      if (remainedUnits >= unitAmount) {
        boolean result = board.deployUnits(territoryId, unitAmount, player.getPlayerId());
        if (result) {
          remainedUnits -= unitAmount;
          player.sendObject(Constant.LEGAL_DEPLOY_INFO);
        } else {
          player.sendObject(Constant.NOT_OWNER_INFO);
        }
      } else {
        player.sendObject("You don't have enough units remained!");
      }
    }
    player.sendObject(Constant.FINISH_DEPLOY_INFO);
  }

  public void receiveOrder() throws IOException, ClassNotFoundException {
    // player side : receive map, send order, receive order info
    // receive another map even if done

    // server side: send map, receive order, check order, send info
    // if get done order, break and send other map
    while (true) {
      player.sendObject(view.displayBoardFor(player.getPlayerId()));
      Order<T> order = (Order<T>) player.receiveObject();
      String message = null;
      if (order instanceof DoneOrder) {
        player.sendObject(Constant.LEGAL_ORDER_INFO);
        break;
      } else if (order instanceof MoveOrder) {
        message = moveChecker.checkOrder(player.getPlayerId(), order, board);
      } else if (order instanceof AttackOrder) {
        message = attackChecker.checkOrder(player.getPlayerId(), order, board);
      }
      if (message == null) {
        order.execute(board);
        player.sendObject(Constant.LEGAL_ORDER_INFO);
      } else {
        player.sendObject(message);
      }
    }
    player.sendObject(view.displayBoardFor(player.getPlayerId()));
  }

  public boolean toEnd() throws IOException, ClassNotFoundException, InterruptedException, BrokenBarrierException {
    switch (player.getPlayerStatus()) {
    case Constant.SELF_NOT_LOSE_NO_ONE_WIN_STATUS:
      player.sendObject(Constant.NOT_LOSE_INFO);
      return false;
    case Constant.SELF_LOSE_NO_ONE_WIN_STATUS:
      player.sendObject(Constant.LOSE_INFO);
      loseChoice();
      return true;
    case Constant.SELF_WIN_STATUS:
      player.sendObject(Constant.WIN_INFO);
      doEndPhase();
      return true;
    case Constant.SELF_LOSE_OTHER_WIN_STATUS:
      player.sendObject(Constant.GAME_END_INFO);
      doEndPhase();
      return true;
    default:
      return false;
    }
  }

  public void loseChoice() throws IOException, ClassNotFoundException, InterruptedException, BrokenBarrierException {
    String choice = (String) player.receiveObject();
    switch (choice) {
    case Constant.TO_WATCH_INFO:
      doWatchPhase();
      return;
    case Constant.TO_QUIT_INFO:
      doEndPhase();
      return;
    default:
      return;
    }
  }

  public void doWatchPhase() throws IOException, InterruptedException, BrokenBarrierException, ClassNotFoundException {
    player.sendObject(Constant.CONFIRM_INFO);
    while (true) {
      barrier.await();
      barrier.await();
      player.sendObject(view.displayFullBoard());
      if (player.getPlayerStatus() == Constant.SELF_LOSE_OTHER_WIN_STATUS) {
        player.sendObject(Constant.GAME_END_INFO);
        break;
      } else {
        player.sendObject(Constant.GAME_CONTINUE_INFO);
      }
    }
    player.receiveObject();
    doEndPhase();
  }

  public void doEndPhase() throws IOException, InterruptedException, BrokenBarrierException {
    player.sendObject(Constant.CONFIRM_INFO);
    while (true) {
      int status = player.getPlayerStatus();
      if (status == Constant.SELF_WIN_STATUS || status == Constant.SELF_LOSE_OTHER_WIN_STATUS) {
        break;
      }
      barrier.await();
      barrier.await();
    }
  }

  @Override
  public void run() {
    try {
      pickTerritory();
      deployUnits();
      while (true) {
        // BUG: add a check lose here, if current player lose, dont do receiveOrder, but directly barrier.await()
        receiveOrder();
        barrier.await();
        barrier.await();
        player.sendObject(view.displayFullBoard());
        if (toEnd()) {
          break;  // BUG: do not break here, but to continue
        }
      }
    } catch (IOException e) {
      return;
    } catch (InterruptedException e) {
      return;
    } catch (BrokenBarrierException e) {
      return;
    } catch (ClassNotFoundException e) {
      return;
    }
  }
}
