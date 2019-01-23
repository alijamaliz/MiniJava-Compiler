package semantic.symbol;

public class Symbol{
    public SymbolType type;
    public int address;
    public Symbol(SymbolType type , int address)
    {
        this.type = type;
        this.address = address;
    }
}
