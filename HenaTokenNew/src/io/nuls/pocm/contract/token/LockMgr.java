package io.nuls.pocm.contract.token;


import io.nuls.pocm.contract.ownership.Ownable;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Block;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;
import java.util.*;

import static io.nuls.contract.sdk.Utils.require;

class LockMgr extends Ownable {

    private Map<Address, List<TimeLock>> locks = new HashMap<Address, List<TimeLock>>();
    private Map<Address, LockUserInfo> lockUserInfos = new HashMap<Address, LockUserInfo>();

    private boolean availableTransferOwner = true;
    private boolean stopTranser = false;


    protected final int LOCK_TYPE_NORMAL = 1;
    protected final int LOCK_TYPE_STAKE = 2;
    protected final int LOCK_TYPE_MINING_REWARD =3;

    private class LockUserInfo {
        boolean totalLocked = false;
        String tag = "";

        @Override
        public String toString() {
            return "{totalLocked:" + totalLocked + ",tag:" + tag + "}";
        }
    }

    public class TimeLock {
        int lockType;
        long startTime;
        long endTime;
        BigInteger lockedBalance;

        public TimeLock(int lockType, long startTime, long endTime, BigInteger lockedBalance) {
            this.lockType = lockType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lockedBalance = lockedBalance;
        }

        public boolean equal( TimeLock lock){

            if( this.lockType == lock.lockType && this.endTime == lock.endTime){
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "{lockType:" + lockType +",startTime:" + startTime + ",endTime:" + endTime + ",lockedBalance:" + lockedBalance.toString() + "}";
        }
    }

    protected Set<String> importantAddress = new HashSet<String>();

    LockMgr(Address owner, Address manager) {
       super(owner, manager);
    }

    public void setImportantAddress(String address){
        requireManager(Msg.sender());
        if( importantAddress.contains(address) == false){
            importantAddress.add(address);
        }
    }

    public boolean isImportantAddress(String add){
        if( importantAddress.contains(add)){
            return true;
        }
        return false;
    }


    public boolean finishTransferOwner() {
        requireOwner(Msg.sender());
        availableTransferOwner = false;
        return true;
    }

    @View
    public boolean getAvailableTransferOwner() {
        return availableTransferOwner;
    }

    public boolean stopTransfer() {
        requireManager(Msg.sender());
        stopTranser = true;
        return true;
    }

    public boolean startTransfer() {
        requireManager(Msg.sender());
        stopTranser = false;
        return true;
    }

    public boolean setTag(@Required Address address, @Required String tag) {
        requireManager(Msg.sender());
        LockUserInfo userInfo = lockUserInfos.get(address);
        if (userInfo == null) {
            userInfo = new LockUserInfo();
            lockUserInfos.put(address, userInfo);
        }
        userInfo.tag = tag;
        return true;
    }

    @View
    public String getTag(@Required Address address) {
        LockUserInfo userInfo = lockUserInfos.get(address);
        if (userInfo == null) {
            return "";
        }
        return userInfo.tag;
    }

    @View
    public boolean getStopTransfer() {
        return stopTranser;
    }

    public boolean lock(@Required Address targetAddress) {
        requireManager(Msg.sender());
        LockUserInfo userInfo = lockUserInfos.get(targetAddress);
        if (userInfo == null) {
            userInfo = new LockUserInfo();
            lockUserInfos.put(targetAddress, userInfo);
        }
        userInfo.totalLocked = true;
        return true;
    }

    public boolean unlock(@Required Address targetAddress) {
        requireManager(Msg.sender());
        LockUserInfo userInfo = lockUserInfos.get(targetAddress);
        if (userInfo == null) {
            return false;
        }
        userInfo.totalLocked = false;
        return true;
    }

    protected boolean addLock(int lockType, Address targetAddress, BigInteger balance, long startTime, long endTime, int persentage) {
        require(persentage > 0 && persentage <= 100 , "persentage is not in range");
        require(getTime() < endTime , "EndTime should be the future.");

        List<TimeLock> lockList = locks.get(targetAddress);
        if (lockList == null) {
            lockList = new ArrayList<TimeLock>();
            locks.put(targetAddress, lockList);
        }

        BigInteger lockBalance = balance.multiply(BigInteger.valueOf(persentage)).divide(BigInteger.valueOf(100));
        lockList.add(new TimeLock(lockType, startTime, endTime, lockBalance));

        return true;
    }

    protected boolean addLock(int lockType,  Address targetAddress, long endTime, BigInteger lockBalance) {

        require(getTime() < endTime, getTime()+" EndTime should be the future.");
        List<TimeLock> lockList = locks.get(targetAddress);
        if (lockList == null) {
            lockList = new ArrayList<TimeLock>();
            locks.put(targetAddress, lockList);
        }
        lockList.add(new TimeLock(lockType, 0, endTime, lockBalance));
        return true;
    }

    public int removeLock(@Required int lockType, @Required Address targetAddress, @Required long endTime){
        requireManager(Msg.sender());
        List<TimeLock> lockList = locks.get(targetAddress);

        require(lockList != null , "There is no LockData");

        TimeLock lock = new TimeLock(lockType,0, endTime, BigInteger.valueOf(0));
        int result = 0;
        for(Iterator<TimeLock> it = lockList.iterator(); it.hasNext() ; )
        {
            TimeLock value = it.next();
            if(value.equal(lock))
            {
                it.remove();
                result++;
            }
        }
        return result;
    }

    @View
    public String getLockState(@Required Address address){
        String result="{";
        LockUserInfo userInfo = lockUserInfos.get(address);
        if( userInfo != null){

            result += "userInfo:"+userInfo.toString()+",";
        }

        List<TimeLock> lockList = locks.get(address);
        if (lockList != null) {
            result += "locks:[";
            for(TimeLock lock : lockList){
                result += lock.toString() +",";
            }
            result += "]";
        }
        return result+="}";
    }


    @View
    protected BigInteger getLockBalance(Address targetAddress, BigInteger balance) {
        LockUserInfo userInfo = lockUserInfos.get(targetAddress);
        if (userInfo != null) {
            if (userInfo.totalLocked == true) {
                return balance;
            }
        }
        BigInteger lockedBalance = BigInteger.ZERO;
        List<TimeLock> lockList = locks.get(targetAddress);
        if (lockList == null) {
            return lockedBalance;
        }
        long currentTime = getTime();
        for (TimeLock lock : lockList) {
            if (currentTime >= lock.startTime && currentTime <= lock.endTime) {
                lockedBalance = lockedBalance.add(lock.lockedBalance);
            }
        }

        if( lockedBalance.compareTo(balance) > 0){
            return balance;
        }

        return lockedBalance;
    }

    @View
    protected long getTime() {
        return Block.timestamp()/1000;
    }


}


