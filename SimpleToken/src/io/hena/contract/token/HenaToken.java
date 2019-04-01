package io.hena.contract.token;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Block;
import io.nuls.contract.sdk.Contract;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

public class HenaToken extends LockMgr implements Contract, Token {

    private final String name;
    private final String symbol;
    private final int decimals;
    private BigInteger totalSupply;

    private Map<Address, BigInteger> balances = new HashMap<Address, BigInteger>();
    private Map<Address, Map<Address, BigInteger>> allowed = new HashMap<Address, Map<Address, BigInteger>>();


    @Override
    @View
    public String name() {
        return name;
    }

    @Override
    @View
    public String symbol() {
        return symbol;
    }

    @Override
    @View
    public int decimals() {
        return decimals;
    }

    @Override
    @View
    public BigInteger totalSupply() {
        return totalSupply;
    }

    public HenaToken(@Required String name, @Required String symbol, @Required String owner, @Required String manager) {
        super(new Address(owner), new Address(manager));

        BigInteger initialAmount = BigInteger.valueOf(1000);
        this.name = name;
        this.symbol = symbol;
        this.decimals = 8;
        this.totalSupply = initialAmount.multiply(BigInteger.TEN.pow(decimals));
        balances.put(new Address(owner), totalSupply);
        emit(new TransferEvent(null, Msg.sender(), totalSupply));

    }

    @Override
    @View
    public BigInteger allowance(@Required Address owner, @Required Address spender) {
        Map<Address, BigInteger> ownerAllowed = allowed.get(owner);
        if (ownerAllowed == null) {
            return BigInteger.ZERO;
        }
        BigInteger value = ownerAllowed.get(spender);
        if (value == null) {
            value = BigInteger.ZERO;
        }
        return value;
    }

    @Override
    @View
    public BigInteger balanceOf(@Required Address owner) {
        require(owner != null);
        BigInteger balance = balances.get(owner);
        if (balance == null) {
            balance = BigInteger.ZERO;
        }
        return balance;
    }

    @Override
    public boolean approve(@Required Address spender, @Required BigInteger value) {
        setAllowed(Msg.sender(), spender, value);
        emit(new ApprovalEvent(Msg.sender(), spender, value));
        return true;
    }

    @Override
    public boolean transferFrom(@Required Address from, @Required Address to, @Required BigInteger value) {
        require(!getStopTransfer(), "stoped transfer" );

        subtractAllowed(from, Msg.sender(), value);
        subtractBalance(from, value);

        addBalance(to, value);

        emit(new TransferEvent(from, to, value));
        return true;
    }

    @Override
    public boolean transfer(@Required Address to, @Required BigInteger value) {
        require(!getStopTransfer(), "stoped transfer" );

        subtractBalance(Msg.sender(), value);

        addBalance(to, value);

        emit(new TransferEvent(Msg.sender(), to, value));
        return true;
    }

    public boolean transferOwner(@Required Address from, @Required Address to, @Required BigInteger value) {
        require(getAvailableTransferOwner(), "finished transferOwner");
        requireOwner(Msg.sender());

        subtractBalance(from, value);

        addBalance(to, value);

        emit(new TransferOwnerEvent(from, to, value));
        return true;
    }

    public boolean burn(@Required BigInteger burnValue) {
        requireOwner(Msg.sender());

        subtractBalance(Msg.sender(), burnValue);

        totalSupply = totalSupply.subtract(burnValue);
        emit(new BurnEvent(burnValue));
        return true;
    }

    private void subtractAllowed(Address address1, Address address2, BigInteger value) {
        BigInteger allowance = allowance(address1, address2);
        check(allowance, value, "Insufficient approved token");
        setAllowed(address1, address2, allowance.subtract(value));
    }

    private void setAllowed(Address address1, Address address2, BigInteger value) {
        check(value);
        Map<Address, BigInteger> address1Allowed = allowed.get(address1);
        if (address1Allowed == null) {
            address1Allowed = new HashMap<Address, BigInteger>();
            allowed.put(address1, address1Allowed);
        }
        address1Allowed.put(address2, value);
    }

    private void addBalance(Address address, BigInteger value) {
        BigInteger balance = balanceOf(address);
        check(value, "The value must be greater than or equal to 0.");
        check(balance);
        balances.put(address, balance.add(value));
    }

    private void subtractBalance(Address address, BigInteger value) {
        BigInteger totalBalance = balanceOf(address);
        BigInteger availableBalance = availableBalanceOf(address);

        check(availableBalance, value, "Insufficient balance of token.");
        balances.put(address, totalBalance.subtract(value));
    }

    private void check(BigInteger value) {
        require(value != null && value.compareTo(BigInteger.ZERO) >= 0);
    }
    private void check(BigInteger value1, BigInteger value2) {
        check(value1);
        check(value2);
        require(value1.compareTo(value2) >= 0);
    }
    private void check(BigInteger value, String msg) {
        require(value != null && value.compareTo(BigInteger.ZERO) >= 0, msg);
    }
    private void check(BigInteger value1, BigInteger value2, String msg) {
        check(value1);
        check(value2);
        require(value1.compareTo(value2) >= 0, msg);
    }

    @View
    public BigInteger availableBalanceOf(@Required Address owner) {
        BigInteger totalBalance = balanceOf(owner);
        BigInteger lockedBalance = getLockBalance(owner, totalBalance);
        return totalBalance.subtract(lockedBalance);
    }

    public boolean addLockNormal(@Required Address targetAddress, String[] startTime, String[] endTime, String[] persentage) {
        requireManager(Msg.sender());
        check(balanceOf(targetAddress));
        require(startTime.length == endTime.length && startTime.length == persentage.length);
        for (int i = 0; i < startTime.length; i++) {
            addLock(LOCK_TYPE_NORMAL, targetAddress, balanceOf(targetAddress), Long.parseLong(startTime[i]), Long.parseLong(endTime[i]), Integer.parseInt(persentage[i]));
        }

        return true;
    }
    protected boolean removeLockNormal(@Required Address targetAddress, @Required long endTime) {
        requireManager(Msg.sender());
        removeLock(LOCK_TYPE_NORMAL, targetAddress, endTime);
        return true;
    }

    public boolean addLockStake(@Required String endTime, @Required BigInteger value) {
        check(balanceOf(Msg.sender()));
        check(value);
        addLock(LOCK_TYPE_STAKE, Msg.sender(), value, Block.timestamp(), Long.parseLong(endTime), 100);
        return true;
    }
    public boolean removeLockStake(@Required long endTime) {
        removeLock(LOCK_TYPE_STAKE, Msg.sender(), endTime);
        return true;
    }


}
