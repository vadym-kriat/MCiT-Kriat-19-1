package ua.nure.lb5_6

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Pathfinder {
  def calculateAction(percept: WumpusPercept): SpeleologistAction = pathfinder.get_actions(percept)._2

  val pathfinder = new Pathfinder

  val START = "start"
  val WAMPUS = "wampus"
  val PIT = "pit"
  val BREEZE = "breeze"
  val STENCH = "stench"
  val SCREAM = "scream"
  val GOLD = "gold"
  val BUMP = "bump"
  var ROOM_STATUS_TRUE = 1
  var ROOM_STATUS_FALSE = 2
  var ROOM_STATUS_POSSIBLE = 3
  var ROOM_STATUS_NO_GOLD_WAY = 4
  var ROOM_STATUS_NO_STATUS: Int = -1
}

class Pathfinder {
  private var agentPosition = Position(0, 0)
  private var agentsWayStory = mutable.ListBuffer[(Int, Int)]()
  private var moveRoom = false
  private var agentX = 0
  private var agentY = 0
  var world: ImaginaryWampusWorld = new ImaginaryWampusWorld

  private def get_actions(wumpusPercept: WumpusPercept) = {
    System.out.println("Agent pos before: " + agentPosition.x + " | " + agentPosition.y)
    var actions: (Look, SpeleologistAction) = null
    var checking_room = world.worldGrid(agentPosition)
    if (checking_room == null) {
      checking_room = new ImaginaryRoom
      world.worldGrid = world.worldGrid + (agentPosition -> checking_room)
    }
    val room_info = Array[String]()
    if (room_info.contains(Pathfinder.BUMP)) {
      val agentStory = agentsWayStory
      agentStory.addOne((agentPosition.x, agentPosition.y))
      agentPosition.x = agentX
      agentPosition.y = agentY
      if (world.worldGrid(agentPosition).exist != Pathfinder.ROOM_STATUS_TRUE) {
        world.worldGrid(agentPosition).exist = Pathfinder.ROOM_STATUS_TRUE
        System.out.println("MARKED THE EXISTENCE")
      }
      moveRoom = false
    }
    else {
      val helpPosition = Position(agentX, agentY)
      world.worldGrid(helpPosition).exist = Pathfinder.ROOM_STATUS_FALSE
    }
    checking_room = world.worldGrid(agentPosition)
    if (checking_room == null) {
      checking_room = new ImaginaryRoom
      world.worldGrid = world.worldGrid + (agentPosition -> checking_room)
    }
    if (checking_room.ok != Pathfinder.ROOM_STATUS_TRUE) checking_room.ok = Pathfinder.ROOM_STATUS_TRUE
    for (event <- room_info) {
      checking_room.addEvent(event)
    }
    updateNeighbors(agentPosition)
    if (world.isWampusAlive && world.wampusRoomCount > 2) {
      val wampusPosition = world.getWampusCoords
      actions = getNextRoomAction(agentPosition, wampusPosition, Shoot)
    }
    else {
      val nextOkRooms = getOkNeighbors(agentPosition)
      var best_candidate = -1
      var candidate_status = -1
      for (i <- nextOkRooms.indices) {
        val candidate_room = nextOkRooms(i)
        System.out.println("CANDIDATE CHECKING: " + candidate_room.x + " " + candidate_room.y)
        System.out.println("AGENT CHECKING: " + agentPosition.x + " " + agentPosition.y)
        if (candidate_room.x > agentPosition.x) {
          best_candidate = i
          System.out.println("1")
        }
        else if (candidate_room.y > agentPosition.y) if (candidate_status < 3) {
          System.out.println("2")
          candidate_status = 3
        }
        else if (candidate_room.x < agentPosition.x) if (candidate_status < 2) {
          System.out.println("3")
          candidate_status = 2
        }
        else if (candidate_status < 1) {
          System.out.println("4")
          candidate_status = 1
        }
        best_candidate = i
      }
      actions = getNextRoomAction(agentPosition, nextOkRooms(best_candidate), Forward)
    }

    actions
  }

  private def getNextRoomAction(agentPosition: Position, nextOkRoom: Position, action: SpeleologistAction): (Look, SpeleologistAction) = {
    agentX = agentPosition.x
    agentY = agentPosition.y
    var look: Look = LookUp
    if (agentPosition.y < nextOkRoom.y) {
      agentY += 1
      look = LookUp
    }
    else if (agentPosition.y > nextOkRoom.y) {
      agentY -= 1
      look = LookDown
    }
    else if (agentPosition.x < nextOkRoom.x) {
      agentX += 1
      look = LookRight
    }
    else {
      agentX -= 1
      look = LookLeft
    }
    moveRoom = true
    (look, action)
  }

  private def getOkNeighbors(agentPosition: Position): List[Position] = {
    val okNeighbors = getNeighborsPosition(agentPosition)
    val okPositions = ListBuffer[Position]()
    for (position <- okNeighbors) {
      this.world.worldGrid = this.world.worldGrid + (position -> new ImaginaryRoom)
      if ((this.world.worldGrid(position).ok == Pathfinder.ROOM_STATUS_TRUE
        && this.world.worldGrid(position).noWay != Pathfinder.ROOM_STATUS_TRUE
        && this.world.worldGrid(position).exist != Pathfinder.ROOM_STATUS_FALSE)
        || this.world.worldGrid(position).ok == Pathfinder.ROOM_STATUS_NO_STATUS) okPositions += position
    }
    if (okPositions.isEmpty) {
      val (x: Int, y: Int) = agentsWayStory.last
      okPositions.addOne(Position(x, y))
      this.world.worldGrid(agentPosition).noWay = Pathfinder.ROOM_STATUS_TRUE
    }
    okPositions.toList
  }

  private def getNeighborsImaginaryRoom(agentPosition: Position) = {
    val rightNeighbor = Position(agentPosition.x + 1, agentPosition.y)
    val upNeighbor = Position(agentPosition.x, agentPosition.y + 1)
    val leftNeighbor = Position(agentPosition.x - 1, agentPosition.y)
    val bottomNeighbor = Position(agentPosition.x, agentPosition.y - 1)
    var rightRoom = world.worldGrid(rightNeighbor)
    if (rightRoom == null) {
      rightRoom = new ImaginaryRoom
      world.worldGrid = world.worldGrid + (rightNeighbor -> rightRoom)
    }
    var upRoom = world.worldGrid(upNeighbor)
    if (upRoom == null) {
      upRoom = new ImaginaryRoom
      world.worldGrid = world.worldGrid + (rightNeighbor -> upRoom)
    }
    var leftRoom = world.worldGrid(leftNeighbor)
    if (leftRoom == null) {
      leftRoom = new ImaginaryRoom
      world.worldGrid = world.worldGrid + (rightNeighbor -> leftRoom)
    }
    var bottomRoom = world.worldGrid(bottomNeighbor)
    if (bottomRoom == null) {
      bottomRoom = new ImaginaryRoom
      world.worldGrid = world.worldGrid + (rightNeighbor -> bottomRoom)
    }
    val rooms = Array[ImaginaryRoom](rightRoom, upRoom, leftRoom, bottomRoom)
    rooms
  }

  private def getNeighborsPosition(agentPosition: Position) = {
    val rightNeighbor = Position(agentPosition.x + 1, agentPosition.y)
    val upNeighbor = Position(agentPosition.x, agentPosition.y + 1)
    val leftNeighbor = Position(agentPosition.x - 1, agentPosition.y)
    val bottomNeighbor = Position(agentPosition.x, agentPosition.y - 1)

    Array[Position](rightNeighbor, upNeighbor, leftNeighbor, bottomNeighbor)
  }

  private def updateNeighbors(agentPosition: Position): Unit = {
    val currentRoom = world.worldGrid(agentPosition)

    val roomList = getNeighborsImaginaryRoom(agentPosition)
    if (currentRoom.stench == Pathfinder.ROOM_STATUS_TRUE) {
      world.wampusRoomCount = world.wampusRoomCount + 1
      for (room <- roomList) {
        if (room.wampus == Pathfinder.ROOM_STATUS_NO_STATUS) {
          room.ok = Pathfinder.ROOM_STATUS_POSSIBLE
          room.wampus = Pathfinder.ROOM_STATUS_POSSIBLE
        }
      }
    }
    if (currentRoom.breeze == Pathfinder.ROOM_STATUS_TRUE) for (room <- roomList) {
      if (room.pit == Pathfinder.ROOM_STATUS_NO_STATUS) {
        room.ok = Pathfinder.ROOM_STATUS_POSSIBLE
        room.pit = Pathfinder.ROOM_STATUS_POSSIBLE
      }
    }
    if (currentRoom.breeze == Pathfinder.ROOM_STATUS_FALSE && currentRoom.stench == Pathfinder.ROOM_STATUS_FALSE) for (room <- roomList) {
      room.ok = Pathfinder.ROOM_STATUS_TRUE
      room.wampus = Pathfinder.ROOM_STATUS_FALSE
      room.pit = Pathfinder.ROOM_STATUS_FALSE
    }
  }
}

class ImaginaryWampusWorld() {
  var worldGrid: Map[Position, ImaginaryRoom] = Map[Position, ImaginaryRoom]()
  var isWampusAlive = true
  var wampusRoomCount = 0
  var wampusCoords: Position = _

  def getWampusCoords: Position = {
    var xWampusCoord = 0
    var yWampusCoord = 0
    val keys = worldGrid.keySet

    for (roomPosition <- keys) {
      val room: Option[ImaginaryRoom] = worldGrid.get(roomPosition)
      if (room.nonEmpty && room.get.wampus == Pathfinder.ROOM_STATUS_POSSIBLE) {
        xWampusCoord += roomPosition.x
        yWampusCoord += roomPosition.y
      }
    }
    xWampusCoord /= wampusRoomCount
    yWampusCoord /= wampusRoomCount
    this.wampusCoords = Position(xWampusCoord, yWampusCoord)

    this.wampusCoords
  }
}

class ImaginaryRoom() {
  var exist: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var stench: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var breeze: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var pit: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var wampus: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var ok: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var gold: Int = Pathfinder.ROOM_STATUS_NO_STATUS
  var noWay: Int = Pathfinder.ROOM_STATUS_NO_STATUS

  def addEvent(event_name: String): Unit = {
    event_name match {
      case Pathfinder.START =>

      case Pathfinder.WAMPUS =>
        this.wampus = Pathfinder.ROOM_STATUS_TRUE

      case Pathfinder.PIT =>
        this.pit = Pathfinder.ROOM_STATUS_TRUE

      case Pathfinder.BREEZE =>
        this.breeze = Pathfinder.ROOM_STATUS_TRUE

      case Pathfinder.STENCH =>
        this.stench = Pathfinder.ROOM_STATUS_TRUE

      case Pathfinder.SCREAM =>

      case Pathfinder.GOLD =>
        this.gold = Pathfinder.ROOM_STATUS_TRUE

      case Pathfinder.BUMP =>

    }
  }
}

case class Position(var x: Int, var y: Int)
