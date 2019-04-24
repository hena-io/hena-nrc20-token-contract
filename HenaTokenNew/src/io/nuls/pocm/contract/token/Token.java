package io.nuls.pocm.contract.token;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;

public interface Token {

    @View
    String name();

    @View
    String symbol();

    @View
    int decimals();

    @View
    BigInteger totalSupply();

    @View
    BigInteger balanceOf(@Required Address owner);

    boolean transfer(@Required Address to, @Required BigInteger value);

    boolean transferFrom(@Required Address from, @Required Address to, @Required BigInteger value);

    boolean approve(@Required Address spender, @Required BigInteger value);


    @View
    BigInteger allowance(@Required Address owner, @Required Address spender);

    class TransferEvent implements Event {

        private Address from;

        private Address to;

        private BigInteger value;

        public TransferEvent(Address from, @Required Address to, @Required BigInteger value) {
            this.from = from;
            this.to = to;
            this.value = value;
        }

        public Address getFrom() {
            return from;
        }

        public void setFrom(Address from) {
            this.from = from;
        }

        public Address getTo() {
            return to;
        }

        public void setTo(Address to) {
            this.to = to;
        }

        public BigInteger getValue() {
            return value;
        }

        public void setValue(BigInteger value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TransferEvent that = (TransferEvent) o;

            if (from != null ? !from.equals(that.from) : that.from != null) return false;
            if (to != null ? !to.equals(that.to) : that.to != null) return false;
            return value != null ? value.equals(that.value) : that.value == null;
        }

        @Override
        public int hashCode() {
            int result = from != null ? from.hashCode() : 0;
            result = 31 * result + (to != null ? to.hashCode() : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TransferEvent{" +
                    "from=" + from +
                    ", to=" + to +
                    ", value=" + value +
                    '}';
        }

    }

//    class TransferPOCMEvent implements Event {
//
//        private Address from;
//
//        private Address to;
//
//        private BigInteger value;
//
//        private Long lockTime;
//
//        public TransferPOCMEvent(Address from, @Required Address to, @Required BigInteger value, @Required long lockTime) {
//            this.from = from;
//            this.to = to;
//            this.value = value;
//            this.lockTime =lockTime;
//        }
//
//        public Address getFrom() {
//            return from;
//        }
//
//        public void setFrom(Address from) {
//            this.from = from;
//        }
//
//        public Address getTo() {
//            return to;
//        }
//
//        public void setTo(Address to) {
//            this.to = to;
//        }
//
//        public BigInteger getValue() {
//            return value;
//        }
//
//        public void setValue(BigInteger value) {
//            this.value = value;
//        }
//
//        public Long getLockTime() { return lockTime; }
//
//        public void setLockTime(long lockTime){
//            this.lockTime = lockTime;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//
//            TransferPOCMEvent that = (TransferPOCMEvent) o;
//
//            if (from != null ? !from.equals(that.from) : that.from != null) return false;
//            if (to != null ? !to.equals(that.to) : that.to != null) return false;
//            if (lockTime != null ? !lockTime.equals(that.lockTime) : that.lockTime != null) return false;
//            return value != null ? value.equals(that.value) : that.value == null;
//    }
//
//        @Override
//        public int hashCode() {
//            int result = from != null ? from.hashCode() : 0;
//            result = 31 * result + (to != null ? to.hashCode() : 0);
//            result = 31 * result + (value != null ? value.hashCode() : 0);
//            result = 31 * result + (lockTime != null ? lockTime.hashCode() : 0);
//            return result;
//        }
//
//        @Override
//        public String toString() {
//            return "TransferEvent{" +
//                    "from=" + from +
//                    ", to=" + to +
//                    ", value=" + value +
//                    ". lockTime=" + lockTime +
//                    '}';
//        }
//
//    }

//    class TransferOwnerEvent implements Event {
//
//        private Address from;
//
//        private Address to;
//
//        private BigInteger value;
//
//        public TransferOwnerEvent(@Required Address from, @Required Address to, @Required BigInteger value) {
//            this.from = from;
//            this.to = to;
//            this.value = value;
//        }
//
//        public Address getFrom() {
//            return from;
//        }
//
//        public void setFrom(Address from) {
//            this.from = from;
//        }
//
//        public Address getTo() {
//            return to;
//        }
//
//        public void setTo(Address to) {
//            this.to = to;
//        }
//
//        public BigInteger getValue() {
//            return value;
//        }
//
//        public void setValue(BigInteger value) {
//            this.value = value;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//
//            TransferOwnerEvent that = (TransferOwnerEvent) o;
//
//            if (from != null ? !from.equals(that.from) : that.from != null) return false;
//            if (to != null ? !to.equals(that.to) : that.to != null) return false;
//            return value != null ? value.equals(that.value) : that.value == null;
//        }
//
//        @Override
//        public int hashCode() {
//            int result = from != null ? from.hashCode() : 0;
//            result = 31 * result + (to != null ? to.hashCode() : 0);
//            result = 31 * result + (value != null ? value.hashCode() : 0);
//            return result;
//        }
//
//        @Override
//        public String toString() {
//            return "TransferOwnerEvent{" +
//                    "from=" + from +
//                    ", to=" + to +
//                    ", value=" + value +
//                    '}';
//        }
//    }

    class BurnEvent implements Event {
        private BigInteger burnValue;

        public BurnEvent(@Required BigInteger burnValue) {
            this.burnValue = burnValue;
        }

        public BigInteger getValue() {
            return burnValue;
        }

        public void setValue(BigInteger value) {
            this.burnValue = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BurnEvent that = (BurnEvent) o;
            return burnValue != null ? burnValue.equals(that.burnValue) : that.burnValue == null;
        }

        @Override
        public int hashCode() {
            int result = burnValue != null ? burnValue.hashCode() : 0;
            return result;
        }

        @Override
        public String toString() {
            return "BurnEvent{" +
                    "burnValue=" + burnValue +
                    '}';
        }
    }

    class ApprovalEvent implements Event {

        private Address owner;

        private Address spender;

        private BigInteger value;

        public ApprovalEvent(@Required Address owner, @Required Address spender, @Required BigInteger value) {
            this.owner = owner;
            this.spender = spender;
            this.value = value;
        }

        public Address getOwner() {
            return owner;
        }

        public void setOwner(Address owner) {
            this.owner = owner;
        }

        public Address getSpender() {
            return spender;
        }

        public void setSpender(Address spender) {
            this.spender = spender;
        }

        public BigInteger getValue() {
            return value;
        }

        public void setValue(BigInteger value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ApprovalEvent that = (ApprovalEvent) o;

            if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
            if (spender != null ? !spender.equals(that.spender) : that.spender != null) return false;
            return value != null ? value.equals(that.value) : that.value == null;
        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + (spender != null ? spender.hashCode() : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ApprovalEvent{" +
                    "owner=" + owner +
                    ", spender=" + spender +
                    ", value=" + value +
                    '}';
        }

    }

}
