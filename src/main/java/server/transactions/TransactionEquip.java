package server.transactions;

public class TransactionEquip extends TransactionItem {
    private int upgradeSlots;
    private byte level;
    private short str;
    private short dex;
    private short intStat;
    private short luk;
    private short hp;
    private short mp;
    private short watk;
    private short matk;
    private short wdef;
    private short mdef;
    private short acc;
    private short avoid;
    private short hands;
    private short speed;
    private short jump;
    private int locked;
    private int vicious;
    private byte itemLevel;
    private int itemExp;
    private int ringId;

    public int getUpgradeSlots() {
        return upgradeSlots;
    }

    public void setUpgradeSlots(int upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public short getStr() {
        return str;
    }

    public void setStr(short str) {
        this.str = str;
    }

    public short getDex() {
        return dex;
    }

    public void setDex(short dex) {
        this.dex = dex;
    }

    public short getIntStat() {
        return intStat;
    }

    public void setIntStat(short intStat) {
        this.intStat = intStat;
    }

    public short getLuk() {
        return luk;
    }

    public void setLuk(short luk) {
        this.luk = luk;
    }

    public short getHp() {
        return hp;
    }

    public void setHp(short hp) {
        this.hp = hp;
    }

    public short getMp() {
        return mp;
    }

    public void setMp(short mp) {
        this.mp = mp;
    }

    public short getWatk() {
        return watk;
    }

    public void setWatk(short watk) {
        this.watk = watk;
    }

    public short getMatk() {
        return matk;
    }

    public void setMatk(short matk) {
        this.matk = matk;
    }

    public short getWdef() {
        return wdef;
    }

    public void setWdef(short wdef) {
        this.wdef = wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public void setMdef(short mdef) {
        this.mdef = mdef;
    }

    public short getAcc() {
        return acc;
    }

    public void setAcc(short acc) {
        this.acc = acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public void setAvoid(short avoid) {
        this.avoid = avoid;
    }

    public short getHands() {
        return hands;
    }

    public void setHands(short hands) {
        this.hands = hands;
    }

    public short getSpeed() {
        return speed;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }

    public short getJump() {
        return jump;
    }

    public void setJump(short jump) {
        this.jump = jump;
    }

    public int getLocked() {
        return locked;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }

    public int getVicious() {
        return vicious;
    }

    public void setVicious(int vicious) {
        this.vicious = vicious;
    }

    public byte getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(byte itemLevel) {
        this.itemLevel = itemLevel;
    }

    public int getItemExp() {
        return itemExp;
    }

    public void setItemExp(int itemExp) {
        this.itemExp = itemExp;
    }

    public int getRingId() {
        return ringId;
    }

    public void setRingId(int ringId) {
        this.ringId = ringId;
    }
}
