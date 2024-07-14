public class AdvectionInterpolationInfo{
    public int CellTLX;
    public int CellTLY;
    public int CellTRX;
    public int CellTRY;
    public int CellBLX;
    public int CellBLY;
    public int CellBRY;
    public int CellBRX;
    public double tx;
    public double ty;

    public AdvectionInterpolationInfo(int cellTLX, int cellTLY, int cellTRX, int cellTRY, int cellBLX, int cellBLY, int cellBRX, int cellBRY, double tx, double ty) {
        CellTLX = cellTLX;
        CellTLY = cellTLY;
        CellTRX = cellTRX;
        CellTRY = cellTRY;
        CellBLX = cellBLX;
        CellBLY = cellBLY;
        CellBRY = cellBRY;
        CellBRX = cellBRX;
        this.tx = tx;
        this.ty = ty;
    }
}
