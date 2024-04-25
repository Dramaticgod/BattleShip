package battleshipserver;

class Point {
    int x;
    int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Letters are rows. Supports A to Z. (length 26)
     * Numbers are columns. Supports 0 to 9. (length 10)
     * <p>
     * A1 becomes (1, 0), because x-values are columns and y-values are rows.
     */
    public static Point parsePoint(String coordinate) {
        char letter = coordinate.charAt(0);
        char number = coordinate.charAt(1);

        return new Point(number - '0', letter - 'A');
    }
}
