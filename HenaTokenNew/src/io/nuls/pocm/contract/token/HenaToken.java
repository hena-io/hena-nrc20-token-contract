package io.nuls.pocm.contract.token;

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

public class HenaToken extends LockMgr implements  Token, Contract {

    private final String name;
    private final String symbol;
    private final int decimals;
    private BigInteger totalSupply;

    private Map<Address, BigInteger> balances = new HashMap<Address, BigInteger>();
    private Map<Address, Map<Address, BigInteger>> allowed = new HashMap<Address, Map<Address, BigInteger>>();

    private Address POCMAddress;

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

    public HenaToken(@Required String name, @Required String symbol, @Required String strOwner, @Required String manager, String[] receiverAddress, long[] receiverAmount) {
        super(new Address(strOwner), new Address(manager));
        Address owner = new Address(strOwner);
        POCMAddress = owner;
        BigInteger initialAmount = BigInteger.valueOf(1000000000);
        this.name = name;
        this.symbol = symbol;
        this.decimals = 8;
        this.totalSupply = initialAmount.multiply(BigInteger.TEN.pow(decimals));

        BigInteger receiverTotalAmount = BigInteger.ZERO;
        BigInteger reciverTotalAmount = BigInteger.ZERO;
        if(receiverAddress!=null && receiverAmount!=null){
            require(receiverAddress.length==receiverAmount.length);

            BigInteger tempReceiverAmount = BigInteger.ZERO;
            for(int i = 0; i< receiverAddress.length; i++){
                Address tempReceiverAddress = new Address(receiverAddress[i]);
                tempReceiverAmount = BigInteger.valueOf(receiverAmount[i]).multiply(BigInteger.TEN.pow(decimals));
                balances.put(tempReceiverAddress, tempReceiverAmount);
                reciverTotalAmount = receiverTotalAmount.subtract(tempReceiverAmount);
                setImportantAddress(receiverAddress[i]);
                emit(new TransferEvent(owner,tempReceiverAddress, tempReceiverAmount));
            }
        }
        balances.put(owner, totalSupply.subtract(reciverTotalAmount));
        emit(new TransferEvent(null,owner, totalSupply.subtract(reciverTotalAmount)));
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
        require(isImportantAddress(from.toString()) == false, from.toString()+" is Important Address");
        subtractBalance(from, value);
        addBalance(to, value);
        //emit(new TransferOwnerEvent(from, to, value));
        emit(new TransferEvent(from, to, value));
        return true;
    }

    public boolean transferPOCM(@Required Address to,@Required BigInteger value,@Required long lockTime){
        require(Msg.sender().equals(POCMAddress), "only POCM Contract address allowed");
        subtractBalance(Msg.sender(), value);
        addBalance(to, value);
        addLock( LOCK_TYPE_MINING_REWARD, to, lockTime, value);
        //emit(new TransferPOCMEvent(Msg.sender(), to, value, lockTime));
        emit(new TransferEvent(Msg.sender(), to, value));
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

    protected void addBalance(Address address, BigInteger value) {
        BigInteger balance = balanceOf(address);
        check(value, "The value must be greater than or equal to 0.");
        check(balance);
        balances.put(address, balance.add(value));
    }

    private void subtractBalance(Address address, BigInteger value) {
        BigInteger totalBalance = balanceOf(address);
        BigInteger availableBalance = availableBalanceOf(address);

        check(availableBalance, value, "There is not enough available balance.");
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
    public BigInteger availableBalanceOf(@Required Address address) {
        BigInteger totalBalance = balanceOf(address);
        BigInteger lockedBalance = getLockBalance(address, totalBalance);
        return totalBalance.subtract(lockedBalance);
    }

    public boolean addLockNormal(@Required Address targetAddress, long[] startTime, long[] endTime, int[] persentage) {
        requireManager(Msg.sender());
        check(balanceOf(targetAddress));
        require(startTime.length == endTime.length && startTime.length == persentage.length);
        for (int i = 0; i < startTime.length; i++) {
            addLock(LOCK_TYPE_NORMAL, targetAddress, balanceOf(targetAddress), startTime[i], endTime[i], persentage[i]);
        }
        return true;
    }
//
//    public int  removeLock(@Required int lockType, @Required Address targetAddress, @Required long endTime) {
//        requireManager(Msg.sender());
//        return removeLock(lockType, targetAddress, endTime);
//    }

    public boolean addLockStake(@Required long endTime, @Required BigInteger value) {
        check(value);
        BigInteger availableBalance =  availableBalanceOf(Msg.sender());
        require( availableBalance.compareTo(value) >= 0, "Not enough available balance.");

        addLock(LOCK_TYPE_STAKE, Msg.sender(), value, Block.timestamp(), endTime, 100);
        return true;
    }

    public boolean removeLockStake(@Required long endTime) {
        removeLock(LOCK_TYPE_STAKE, Msg.sender(), endTime);
        return true;
    }

    public void setPOCMAddress(@Required String POCMContractAddress){
        requireManager(Msg.sender());
        this.POCMAddress = new Address(POCMContractAddress);
    }

    public String getPOCMAddress(){
      return this.POCMAddress.toString();
    }
}
