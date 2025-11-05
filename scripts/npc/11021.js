/* 11021.js — Creator Shop Claim Board
 * Rewards creators in B-Coins (#v3020002#)
 */

const CreatorShopManager = Packages.server.creator_shop.CreatorShopManager;
const BCOIN_ID = 3020002;
const CURRENCY = "BCOIN";

const CREATOR_SHOPS = [
    { npcId:11020, name:"Sunny's Shop", share:0.50, password:"SUNNY123" }
];

function fmt(n){return java.text.NumberFormat.getInstance().format(n);}
function plural(n){return n+" B-Coin"+(n>1?"s":"");}

var s=0,shop=null,pwd=null,reward=0;

function start(){
    var t="#e[ Creator Shop Earnings Board ]#n\r\n";
    for(var i=0;i<CREATOR_SHOPS.length;i++)
        t+="#L"+i+"#View #b"+CREATOR_SHOPS[i].name+"#k#l\r\n";
    cm.sendSimple(t);
}

function action(m,t,sel){
    if(m!=1)return cm.dispose(); s++;
    if(s==1){
        shop=CREATOR_SHOPS[sel]; if(!shop)return cm.dispose();
        cm.sendGetText("Enter password for #b"+shop.name+"#k:");
    }else if(s==2){
        pwd=cm.getText();
        if(pwd!==shop.password){cm.sendOk("❌ Wrong password.");return cm.dispose();}
        var tot=CreatorShopManager.getUnclaimedTotalByCurrency(shop.npcId,CURRENCY);
        if(tot<=0){cm.sendOk("No unclaimed earnings for "+shop.name+".");return cm.dispose();}
        reward=Math.floor(tot*shop.share);
        cm.sendYesNo("✅ Password OK!\r\n\r\nTotal: #b"+fmt(tot)+"#k "+plural(tot)+
                     "\r\nYour share ("+(shop.share*100)+"%): #b"+fmt(reward)+
                     "#k "+plural(reward)+"\r\n\r\nClaim now?");
    }else if(s==3){
        if(reward<=0)return cm.dispose();
        cm.gainItem(BCOIN_ID,reward);
        CreatorShopManager.markClaimed(shop.npcId);
        cm.sendOk("💰 Claimed #b"+fmt(reward)+"#k "+plural(reward)+
                  " from #b"+shop.name+"#k!\r\nThank you!");
        cm.dispose();
    }
}
