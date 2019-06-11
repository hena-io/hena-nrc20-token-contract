package io.hena.token;

        import io.nuls.contract.sdk.Address;
        import static io.nuls.contract.sdk.Utils.require;

public class Ownable {

    protected Address owner;
    protected Address manager;

    public Ownable(Address owner, Address manager) {
        this.owner = owner;
        this.manager = manager;
    }

    protected void requireOwner(Address address) {
        require(owner.equals(address), "Only owners are allowed");
    }

    protected void requireManager(Address address) {
        require(address.equals(owner) || address.equals(manager), "Owners or manager are allowed");
    }
}
