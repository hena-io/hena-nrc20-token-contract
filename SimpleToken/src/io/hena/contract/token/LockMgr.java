package io.hena.contract.token;


import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Block;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;
import java.util.*;

import static io.nuls.contract.sdk.Utils.require;

class LockMgr {

    private Map<Address, List<TimeLock>> locks = new HashMap<Address, List<TimeLock>>();
    private Map<Address, LockUserInfo> lockUserInfos = new HashMap<Address, LockUserInfo>();

    private Address owner;
    private Address manager;

    private boolean availableTransferOwner = true;

    private boolean stopTranser = false;

    private class LockUserInfo {
        boolean totalLocked = false;
        String tag = "";

        @Override
        public String toString() {
            return String.format("{totalLocked:%s,tag:%s}", totalLocked, tag);
        }
    }

    public class TimeLock {
        long startTime;
        long endTime;
        BigInteger lockedBalance;

        public TimeLock(long startTime, long endTime, BigInteger lockedBalance) {

            this.startTime = startTime;
            this.endTime = endTime;
            this.lockedBalance = lockedBalance;
        }

        public boolean equal(TimeLock lock) {

            if (this.endTime != lock.endTime)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return String.format("{startTime:%s,endTime:%s,lockedBalance:%s}", startTime, endTime, lockedBalance);
        }
    }

    LockMgr(Address owner, Address manager) {
        this.owner = owner;
        this.manager = manager;
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

    protected void requireOwner(Address address) {
        require(owner.equals(address), "Only owners are allowed");
    }

    protected void requireManager(Address address) {
        require(address.equals(owner) || address.equals(manager), "owners or manager are allowed");
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

    protected boolean addLock(Address targetAddress, BigInteger balance, long startTime, long endTime, int persentage) {
        require(persentage > 0 && persentage <= 100);
        require(getTime() < endTime);

        List<TimeLock> lockList = locks.get(targetAddress);
        if (lockList == null) {
            lockList = new ArrayList<TimeLock>();
            locks.put(targetAddress, lockList);
        }

        BigInteger lockBalance = balance.multiply(BigInteger.valueOf(persentage)).divide(BigInteger.valueOf(100));
        lockList.add(new TimeLock(startTime, endTime, lockBalance));

        return true;
    }

    public boolean removeLock(@Required Address targetAddress, @Required int endTime) {
        requireManager(Msg.sender());
        List<TimeLock> lockList = locks.get(targetAddress);
        require(lockList != null);
        boolean isChange = false;
        for (int i = lockList.size() - 1; i >= 0; i--) {
            if (lockList.get(i).endTime == endTime) {
                lockList.remove(i);
                isChange = true;
            }
        }

        require(isChange);

        return true;
    }

    @View
    public String getLockState(@Required Address address) {
        String result = "";
        LockUserInfo userInfo = lockUserInfos.get(address);
        if (userInfo != null) {
            result += String.format("userinfo:%s", userInfo.toString());
        }

        List<TimeLock> lockList = locks.get(address);
        if (lockList != null) {
            String lockStr = "";
            for (int i = 0; i < lockList.size(); i++) {
                if (i > 0) {
                    lockStr += ",";
                }
                lockStr += lockList.get(i).toString();
            }
            if (userInfo != null)
                result += ",";
            result += String.format("locks:%s", lockStr);
        }

        if (result.isEmpty())
            return "";
        return String.format("{%s}", result);
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
        return lockedBalance;
    }

    @View
    protected long getTime() {
        return Block.timestamp();
    }
    @View
    protected String getOwner() {
        return owner.toString();
    }
    @View
    protected String getManager() {
        return manager.toString();
    }


}


