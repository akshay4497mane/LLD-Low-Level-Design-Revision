//BattleShip LLD Practice Coding

import java.util.*;
class Coordinate{
    int row, col;
    Coordinate(int x, int y){
        this.row = x; this.col = y;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate)) return false;

        Coordinate b = (Coordinate) o;
        return this.row == b.row && this.col == b.col;
    }
    @Override
    public int hashCode(){
        return Objects.hash(row, col);
    }
}
class Ship{
    String id;
    Set<Coordinate> occupiedCells = new HashSet<>();
    boolean destroyed = false;
    Ship(String id){
        this.id = id;
    }
}
class Player{
    String name;
    List<Ship> ships = new ArrayList<>();
    Set<Coordinate> firedShots = new HashSet<>();
    Player( String name){
        this.name = name;
    }
    boolean hasAliveShips(){
        return ships.stream().anyMatch(s -> !s.destroyed);        
    }
}
class Board{
    int size;
    Map<Coordinate, Ship> cellShipMap = new HashMap<>();
    Board(int size){
        this.size=size;
    }
}

class ShipFactory{
    //Extendible , We can support Rectangle, L size Ships in future
    public static Ship createSquareShip( String id ){
        return new Ship(id);
    }
}
class PlacementValidator{
    //validate inside board and no overlap
    static void validateCells(Board board, Set<Coordinate> cells){
        for(Coordinate c : cells){
            if( c.row<0 ||c.col<0 || c.row >= board.size || c.col>=board.size)
                throw new IllegalArgumentException("Ship out of bounds");
            if( board.cellShipMap.containsKey(c) )
                throw new IllegalArgumentException("Ship overlap detected");
        }
    }
}
interface FireStrategy{
    Coordinate nextShot( Set<Coordinate> alreadyFired, int boardSize, int colStart, int colEnd);
}
class RandomFireStrategy implements FireStrategy{
    Random rand = new Random();
    @Override
    public Coordinate nextShot( Set<Coordinate> fired, int n, int colStart, int colEnd){
        while( true ){
            Coordinate c = new Coordinate( rand.nextInt(n), colStart + rand.nextInt(colEnd - colStart +1));
            if(!fired.contains(c)) return c;
        }
    }
}
class BattleShipGameService{
    private int size;
    private Board boardA;
    private Board boardB;
    private Player playerA = new Player("Player A");
    private Player playerB = new Player("Player B");
    private FireStrategy fireStrategy;
    public BattleShipGameService( FireStrategy strategy){
        fireStrategy = strategy;
    }
    public void initGame(int n){
        this.size = n;
        boardA = new Board(n);
        boardB = new Board(n);
        System.out.println("Game initialized");
    }
    public void addShip(String id, int shipSize, int ax, int ay, int bx, int by){
        Ship shipA = ShipFactory.createSquareShip("A-"+id);
        Ship shipB = ShipFactory.createSquareShip("B-"+id);
        placeSquareShip( boardA, shipA, shipSize, ax, ay);
        placeSquareShip( boardB, shipB, shipSize, bx, by);
        playerA.ships.add(shipA);
        playerB.ships.add(shipB);
        System.out.println("Ship with id " + id + "placed for both players.");        
    }
    private void placeSquareShip(Board board, Ship ship, int shipSize, int cx, int cy){
        int half = shipSize/2;
        Set<Coordinate> cells = new HashSet<>();
        for( int r =cx-half; r<=cx+half; r++ ){
            for(int c=cy-half; c<= cy+half; c++){
                cells.add(new Coordinate(r, c));
            }
        }
        PlacementValidator.validateCells(board, cells);
        for(Coordinate cell : cells){
            board.cellShipMap.put(cell, ship);
            ship.occupiedCells.add(cell);
        }
    }
    public void startGame(){
        Player current = new Player("PlayerA");
        Player opponent = new Player("PlayerB");;
        Board opponentBoard = boardB;
        while( playerA.hasAliveShips() && playerB.hasAliveShips() ){
            int colStart = (current == playerA) ? size/2 :0;
            int colEnd = (current == playerA) ? size-1 :size/2+1;
            Coordinate shot = fireStrategy.nextShot(current.firedShots, size, colStart, colEnd);
            current.firedShots.add(shot);
            Ship hitShip = opponentBoard.cellShipMap.get(shot);
            if( hitShip == null){
                System.out.println(current.name + "fire shot at " + shot.row + "," + shot.col + " MISS");
            }else{
                hitShip.destroyed = true;
                System.out.println(current.name + "fire shot at " + shot.row + "," + shot.col + " HIT" + hitShip.id + "DESTROYED");
            }
            if(current == playerA){
                current = playerB;
                opponent = playerA;
                opponentBoard = boardA;
            }else{
                current = playerA;
                opponent = playerB;
                opponentBoard = boardB;
            }
        }
        System.out.println("Game Over = " + ((playerA.hasAliveShips()) ? "PlayerA" : "player B"));
    }
}






/*
initGame(N)

This will initialize the game with a battlefield of size NxN. Where the left half of
N/2xN will be assigned to PlayerA and the right half will be assigned to PlayerB
● addShip(id, size, x position PlayerA, y position PlayerA, x position PlayerB, y position
PlayerB)
This will add a ship of given size at the given coordinates in both the player’s
fleet.
● startGame()

This will begin the game, where PlayerA will always take the first turn. The output
of each step should be printed clearly in the console.
For eg.
PlayerA’s turn: Missile fired at (2, 4). “Hit”. PlayerB’s ship with id “SH1”
destroyed.
PlayerB’s turn: Missile fired at (6, 1). “Miss”

 */

class Main {
    public static void main(String[] args) {
        FireStrategy strategy = new RandomFireStrategy();
        BattleShipGameService game = new BattleShipGameService(strategy);
        game.initGame(6);
        game.addShip("SH1", 2,1,1,4,4);
        game.startGame();
    }
}


/*
API
1. INIT GAME
POST /api/v1/games
{
   boardsize : 6 
}

2.ADD SHIP API
POST /api/v1/games/{gameId}/ships
{
    "shipID" : "SH1",
    "size" : 2,
    "playerA" : {"x":1, "y":1},
    "playerA" : {"x":4, "y":4}, 
}
3.Start GAME API
POST /api/v1/games/{gameId}/start

DATABASE DESIGN:
GAMES
-id(PK)
-board_size
-status   {IN_PROGRESS / FINISHED}
curr_turn_playerid
winner_playerid
created_at

PLAYER
id (PK)
game_id (FK)
name
board_start_col
board_end_col

SHIPS
id (PK)
gameID
player_id
size
destroyed

SHIP_CELL
id (PK)
ship_id (FK)
row
col

FIRED_SHOTS
id (PK)
game_id (FK)
player_id (FK)
x
y
result {HIT/MISS}

*/


/*
## ✅ For Real Production Game
Yes — DB needed for:
* resume game later
* multiplayer remote users
* audit logs
* analytics
* leaderboard
* game history
* crash recovery
* distributed servers

# 📈 Scalable Architecture Version

```
API Layer
   ↓
GameService
   ↓
Repositories
   ↓
Database
```
**How would you persist this game?**

Answer:

> I would store game state in relational tables — games, players, ships, ship_cells, and shots. Each fired shot would be inserted with a unique constraint to prevent duplicate coordinates. Ship cells would be pre-expanded in DB to allow O(1) hit detection via indexed lookup. For performance, I would cache active game state in Redis and use DB as the source of truth.

# ⚖️ SQL vs NoSQL?

## SQL good because:

* strong consistency
* transactions
* uniqueness constraint for shots
* relational mapping fits model

## NoSQL option:

Store full game state as document:

```
game_state_json
```

Good for:

* fast resume
* snapshot saves
* flexible schema

# 🧪 Transaction Needed?

YES — for shot fire:

```
insert shot
update ship
update game turn
```

must be atomic.



*/



