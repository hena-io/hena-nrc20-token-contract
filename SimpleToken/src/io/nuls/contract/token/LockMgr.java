package io.nuls.contract.token;


import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Block;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;
import java.util.*;

import static io.nuls.contract.sdk.Utils.require;

class LockMgr {

    private Map<Address, List<TimeLock>> Locks = new HashMap<Address, List<TimeLock>>();
    private Map<Address, LockUserInfo> lockUserInfos = new HashMap<Address, LockUserInfo>();

    private Address owner;
    private Address manager;

    private boolean availableTransferOwner = true;

    private boolean stopTranser = false;

    private class LockUserInfo{
        boolean totalLocked = false;
        String tag = "";

        @Override
        public String toString(){
            return "{ totalLocked:"+ totalLocked +" tag:"+ tag +"},";
        }
    }

    private class TimeLock {

        long startTime;
        long endTime;
        long lockedTime;
        BigInteger lockedBalance;

        public TimeLock(long startTime, long endTime, BigInteger lockedBalance ){

            this.startTime = startTime;
            this.endTime = endTime;
            this.lockedBalance = lockedBalance;
        }

        public boolean equal( TimeLock lock){

            if( this.endTime != lock.endTime)
                return false;
            return true;
        }

        @Override
        public String toString(){
            return "{ startTime:"+ startTime +" endTime:"+ endTime +" lockedBalance:"+ lockedBalance+"},";
        }

    }

    LockMgr(Address owner, Address manager){

        this.owner = owner;
        this.manager = manager;
    }

    public boolean finishTransferOwner(){
        requireOwner(Msg.sender());
        availableTransferOwner = false;
        return true;
    }



    @View
    public boolean getAvailableTransferOwner(){
        return availableTransferOwner;
    }

    protected void requireOwner(Address address) {
        require(owner.equals(address), "Only owners are allowed");
    }

    protected void requireManager(Address address) {
        require( address == owner || address == manager, "owners or manager are allowed");
    }

    public boolean stopTransfer(){
        requireManager(Msg.sender());
        stopTranser = true;
        return stopTranser;
    }

    public boolean startTransfer(){
        requireManager(Msg.sender());
        stopTranser = false;
        return stopTranser;
    }

    public boolean setTag(@Required Address address, @Required String tag){
        requireManager(Msg.sender());
        LockUserInfo userInfo = lockUserInfos.get(address);
        if( userInfo == null){
            userInfo = new LockUserInfo();
            lockUserInfos.put(address, userInfo);
        }
        userInfo.tag = tag;
        return true;
    }

    @View
    public String getTag(@Required Address address){
        LockUserInfo userInfo = lockUserInfos.get(address);
        if( userInfo == null){
            return "";
        }
        return userInfo.tag;
    }

    @View
    public boolean getStopTransfer(){
        return stopTranser;
    }

    public boolean lock(@Required Address targetAddress){
        requireManager(Msg.sender());
        LockUserInfo userInfo = lockUserInfos.get(targetAddress);
        if( userInfo == null){
            userInfo = new LockUserInfo();
            lockUserInfos.put(targetAddress, userInfo);
        }
        userInfo.totalLocked = true;
        return true;
    }

    public boolean unlock(@Required Address targetAddress){
        requireManager(Msg.sender());
        LockUserInfo userInfo = lockUserInfos.get(targetAddress);
        if( userInfo == null){
            return false;
        }
        userInfo.totalLocked = false;
        return true;
    }

    protected boolean addLock(Address targetAddress,BigInteger balance,  long startTime, long endTime, int persentage){

        if( persentage <= 0 || persentage > 100)
            return false;
        long currentTime = getTime();
        if( currentTime > endTime)
            return false;

        List<TimeLock> lockList = Locks.get(targetAddress);
        if (lockList == null) {
            lockList = new ArrayList<TimeLock>();
        }

        BigInteger lockBalance = balance.multiply(BigInteger.valueOf(100)).divide(BigInteger.valueOf(persentage));
        lockList.add(new TimeLock( startTime, endTime, lockBalance));

        return true;
    }

    public boolean removeLock(@Required Address targetAddress, @Required int endTime){
        requireManager(Msg.sender());
        List<TimeLock> lockList = Locks.get(targetAddress);

        if (lockList == null) {
            return false;
        }

        TimeLock lock = new TimeLock(0, endTime, BigInteger.valueOf(0));
        for(Iterator<TimeLock> it = lockList.iterator(); it.hasNext() ; )
        {
            TimeLock value = it.next();
            if(value.equal(lock))
            {
                it.remove();
            }
        }
        return true;
    }
    @View
    public String getLockState(@Required Address address){
        String result="{";
        LockUserInfo userInfo = lockUserInfos.get(address);
        if( userInfo != null){
            result += userInfo.toString();
        }

        List<TimeLock> lockList = Locks.get(address);
        if (lockList != null) {
            for(TimeLock lock : lockList){
                result += lock.toString();
            }
        }
        return result+="}";
    }

    @View
    protected BigInteger getLockBalance( Address targetAddress, BigInteger balance){

        LockUserInfo userInfo = lockUserInfos.get(targetAddress);
        if(userInfo != null){
            if( userInfo.totalLocked == true){
                return balance;
            }
        }
        BigInteger lockedBalance = BigInteger.ZERO;
        List<TimeLock> lockList = Locks.get(targetAddress);
        if (lockList == null) {
            return lockedBalance;
        }
        long currentTime = getTime();
        for (TimeLock lock : lockList) {

            if( currentTime >= lock.startTime && currentTime <= lock.endTime){
                lockedBalance = lockedBalance.add(lock.lockedBalance);
            }
        }
        return lockedBalance;
    }

    @View
    protected long getTime(){
        return Block.timestamp();
    }
}


