package BattleshipClient;

public class Grid {
    Square[][] squares;
    Ship[] ships;
    int shipsAdded = 0;

    public Grid(int width, int height, int numShips) {
        squares = new Square[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                squares[i][j] = new Square();
            }
        }

        ships = new Ship[numShips];
    }

    public void printBoard(){
        System.out.print("Totale ships : " + ships.length + "\n");
        for (int i = 0; i < squares.length; i++) {
            for (int j = 0; j < squares.length; j++) {
                if (squares[i][j].ship == null  ){
                    System.out.print("~ ");
                }
                else if(squares[i][j].hit){
                    System.out.print("X ");
                }
                else {
                    System.out.print("S ");
                }

            }
            System.out.print("\n");
        }
    }

    public Ship setShip(String coord1, String coord2) {
        Point point1 = Point.parsePoint(coord1);
        Point point2 = Point.parsePoint(coord2);
        return setShip(point1.x, point1.y, point2.x, point2.y);
    }

    public Ship doHit(String coord) {
        Point point = Point.parsePoint(coord);
        return doHit(point.x, point.y);
    }

    /**
     * Provide two coordinates within [0, width) and [0, height) ranges.
     * Either the x-values (column) or y-values (row) need to be equal.
     * This is because the ship must be vertical or horizontal orientation.
     *
     * Ship can be length=1, meaning both coordinates are on the same point.
     */
    public Ship setShip(int x1, int y1, int x2, int y2) {
        if (x1 != x2 && y1 != y2) {
            throw new Error("Either the columns or the rows should be the same of the two coordinates");
        }

        //System.out.println("Start point x : " + x1 + " y : " + y1);
        //System.out.println("End point x : " + x2 + " y : " + y2);


        // A column or a row is already equal, so there is no issue with this
        // Ensure that the 2nd point always has larger values
        int _x1 = Math.min(x1, x2);
        int _y1 = Math.min(y1, y2);
        int _x2 = Math.max(x1, x2);
        int _y2 = Math.max(y1, y2);

        // One column or row is the same, so subtracting those will give you zero
        // The column or row that has different values will subtract properly
        Ship ship = new Ship((_x2 - _x1) + (_y2 - _y1)); // set the length

        // All inclusive, [_y1, _y2] and [_x1, _x2]
        // Set every cell in a horizontal or vertical line to match
        // the same ship object that we created
        //System.out.println("Length of squares array : " + squares.length);
        for (int row = _y1; row <= _y2; row++) {
            for (int col = _x1; col <= _x2; col++) {
               // System.out.println("ROW : " + row + " Column : " + col);
                squares[row][col].ship = ship;
            }
        }

        this.ships[shipsAdded++] = ship;

        return ship;
    }

    /**
     * Returns the ship that was hit. Null if not hit.
     * If ship.health == 0, ship is destroyed.
     */
    public Ship doHit(int x, int y) {
        // Assume that x and y are within [0, width) and [0, height) ranges.
        Square currSquare = squares[y][x];

        if (currSquare.hit) {
            throw new Error("Already hit this square!");
        }

        if (currSquare.ship != null) {
            currSquare.ship.health--;
            currSquare.hit = true;
        }

        return currSquare.ship;
    }

    public int shipsRemaining() {
        int result = 0;
        for (int i = 0; i < ships.length; i++) {
            if (ships[i] != null && ships[i].health >= 0) {
                result++;
            }
        }
        return result;
    }
}

