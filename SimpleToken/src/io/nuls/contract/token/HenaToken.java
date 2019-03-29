package io.nuls.contract.token;

import io.nuls.contract.sdk.Address;
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
    private BigInteger totalSupply = BigInteger.ZERO;

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



    public String test(){

        Address ownerAdd = new Address("TTasNs8MGGGaFT9hd9DLmkammYYv69vs");
        Address manager = new Address("TTanpzeEaBxE2qC8YRnwxiT6zMsPzXhe");
        Address add1 = new Address("TTaqknUWcgomxKX6YPuaUkbZ1JygEMjV");
        Address add2 = new Address("TTau7kAxyhc4yMomVJ2QkMVECKKZK1uG");

        String returnStr = "Hena Token Test Start!!!!\n";

        transfer(add1, BigInteger.valueOf(100));
        transfer(add2, BigInteger.valueOf(200));
        transfer(manager, BigInteger.valueOf(300));

        long currentTime = getTime();
        returnStr +="currentTime:" + currentTime + ", ";
        boolean add1LockResult = addLock(add1, balanceOf(add1),currentTime-10, currentTime + 300, 50 );
        boolean add2LockResult = addLock(add2, balanceOf(add2),currentTime-10, currentTime + 420, 50 );

        returnStr = returnStr + "\n add1LockResult:"+ add1LockResult;
        returnStr = returnStr + "\n add2LockResult:"+ add2LockResult;

        boolean tranferTest1 = transferTest(add1, ownerAdd, BigInteger.valueOf(10));
        boolean tranferTest2 = transferTest(add2, ownerAdd, BigInteger.valueOf(10));

        returnStr = returnStr + "\n tranferTest1:"+ tranferTest1;
        returnStr = returnStr + "\n tranferTest2:"+ tranferTest2;

        return returnStr;
    }

    public HenaToken(@Required String name, @Required String symbol, @Required String owner, @Required String manager) {
        super(new Address(owner), new Address(manager));

        BigInteger initialAmount = BigInteger.valueOf(1000);
        this.name = name;
        this.symbol = symbol;
        this.decimals = 18;
        totalSupply = initialAmount.multiply(BigInteger.TEN.pow(decimals));;
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
    public boolean transferFrom(@Required Address from, @Required Address to, @Required BigInteger value) {

        require(!getStopTransfer(), "stoped transfer" );

        subtractAllowed(from, Msg.sender(), value);
        subtractBalance(from, value);
        addBalance(to, value);
        emit(new TransferEvent(from, to, value));
        return true;
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
    public boolean transfer(@Required Address to, @Required BigInteger value) {

        Address sender = Msg.sender();
        subtractBalance(sender, value);

        addBalance(to, value);
        emit(new TransferEvent(Msg.sender(), to, value));
        return true;
    }

    public boolean transferTest(@Required Address sender, @Required Address to, @Required BigInteger value) {

        subtractBalance(sender, value);
        addBalance(to, value);
        emit(new TransferEvent(Msg.sender(), to, value));
        return true;
    }

    public boolean transferOwner(@Required Address from, @Required Address to, @Required BigInteger value) {
        require( getAvailableTransferOwner(), "finished transferOwner");
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

    public boolean addLock(@Required Address targetAddress,@Required long startTime,@Required long endTime,@Required int persentage){
        requireManager(Msg.sender());
        return addLock(targetAddress, balanceOf(targetAddress), startTime, endTime, persentage);
    }

    public BigInteger getLockBalance(@Required Address targetAddress) {
        return getLockBalance( targetAddress, balanceOf(targetAddress));
    }

    @Override
    public boolean approve(@Required Address spender, @Required BigInteger value) {
        setAllowed(Msg.sender(), spender, value);
        emit(new ApprovalEvent(Msg.sender(), spender, value));
        return true;
    }

    public boolean increaseApproval(@Required Address spender, @Required BigInteger addedValue) {
        addAllowed(Msg.sender(), spender, addedValue);
        emit(new ApprovalEvent(Msg.sender(), spender, allowance(Msg.sender(), spender)));
        return true;
    }

    public boolean decreaseApproval(@Required Address spender, @Required BigInteger subtractedValue) {
        check(subtractedValue);
        BigInteger oldValue = allowance(Msg.sender(), spender);
        if (subtractedValue.compareTo(oldValue) > 0) {
            setAllowed(Msg.sender(), spender, BigInteger.ZERO);
        } else {
            subtractAllowed(Msg.sender(), spender, subtractedValue);
        }
        emit(new ApprovalEvent(Msg.sender(), spender, allowance(Msg.sender(), spender)));
        return true;
    }

    private void addAllowed(Address address1, Address address2, BigInteger value) {
        BigInteger allowance = allowance(address1, address2);
        check(allowance);
        check(value);
        setAllowed(address1, address2, allowance.add(value));
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
        BigInteger lockedBalance = getLockBalance(address);
        BigInteger availableBalance = totalBalance.subtract(lockedBalance);
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



}
