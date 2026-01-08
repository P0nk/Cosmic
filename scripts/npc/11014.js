/* NXT Coin Exchange - Dynamic Market Edition
    NPC: Mr. Hong's Broker Brother
*/

var status = 0;
var NXT_COIN_ID = 3020001;
var PriceManager = Java.type("server.economy.NXTPriceManager"); // Adjust package path if needed

var modeType = -1; // 0 = buy, 1 = sell
var exchangeAmount = 0;
var currentBuyRate = 0;
var currentSellRate = 0;

function start() {
    status = 0;
    
    // Fetch live market data
    var pm = PriceManager.getInstance();
    currentBuyRate = pm.getBuyPrice();
    currentSellRate = pm.getSellPrice();
    var trend = pm.getTrendIcon();

    var msg = "#e#d[NXT Commodities Exchange]#n#k\r\n";
    msg += "The market is moving fast, my friend! " + trend + "\r\n";
    msg += "Prices fluctuate based on supply and demand. Act now!\r\n\r\n";
    
    msg += "Current Market Rates:\r\n";
    msg += "  #gBUY:#k  " + formatNumber(currentBuyRate) + " NX / coin\r\n";
    msg += "  #rSELL:#k " + formatNumber(currentSellRate) + " NX / coin\r\n\r\n";
    
    msg += "#L0#Buy NXT Coin (Invest)#l\r\n";
    msg += "#L1#Sell NXT Coin (Liquidate)#l";
    
    cm.sendSimple(msg);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    status++;

    if (status === 1) {
        modeType = selection;
        
        // Refresh prices one more time to ensure they haven't shifted in the last 2 seconds
        var pm = PriceManager.getInstance();
        currentBuyRate = pm.getBuyPrice();
        currentSellRate = pm.getSellPrice();

        if (modeType === 0) {
            cm.sendGetText("Market Price: #b" + formatNumber(currentBuyRate) + " NX#k per coin.\r\n" +
                           "Your Balance: #d" + formatNumber(getNxCredit()) + " NX#k\r\n\r\n" +
                           "How many NXT Coins would you like to BUY?");
        } else {
            cm.sendGetText("Market Bid: #b" + formatNumber(currentSellRate) + " NX#k per coin.\r\n" +
                           "You hold: #d" + cm.itemQuantity(NXT_COIN_ID) + " coins#k\r\n\r\n" +
                           "How many NXT Coins would you like to SELL?");
        }
    } else if (status === 2) {
        exchangeAmount = parseInt(cm.getText());

        if (isNaN(exchangeAmount) || exchangeAmount <= 0 || exchangeAmount > 1000) {
            cm.sendOk("We only handle orders between 1 and 1000 coins at a time.");
            cm.dispose();
            return;
        }

        var pm = PriceManager.getInstance();

        if (modeType === 0) { // BUY LOGIC
            var totalNXCost = exchangeAmount * currentBuyRate;
            
            if (getNxCredit() < totalNXCost) {
                cm.sendOk("Insufficient funds! You need #r" + formatNumber(totalNXCost) + " NX#k.");
                cm.dispose();
                return;
            }

            // Transaction
            cm.gainCash(-totalNXCost);
            cm.gainItem(NXT_COIN_ID, exchangeAmount);
            
            // Impact Market: Buying raises the price
            pm.recordTransaction(exchangeAmount, true);

            cm.sendCashNoti("Bought " + exchangeAmount + " NXT for " + totalNXCost + " NX.");
            cm.sendOk("Order Filled!\r\nBought #b" + exchangeAmount + " NXT#k.\r\n" +
                      "Your massive purchase has pushed the market price #rUP#k!");
            
        } else { // SELL LOGIC
            if (!cm.haveItem(NXT_COIN_ID, exchangeAmount)) {
                cm.sendOk("You don't have that many coins to sell.");
                cm.dispose();
                return;
            }

            var totalNXGain = exchangeAmount * currentSellRate;

            // Transaction
            cm.gainItem(NXT_COIN_ID, -exchangeAmount);
            cm.gainCash(totalNXGain);

            // Impact Market: Selling lowers the price
            pm.recordTransaction(exchangeAmount, false);

            cm.sendCashNoti("Sold " + exchangeAmount + " NXT for " + totalNXGain + " NX.");
            cm.sendOk("Liquidated!\r\nSold #b" + exchangeAmount + " NXT#k.\r\n" +
                      "Selling this volume has driven the market price #bDOWN#k.");
        }

        cm.dispose();
    }
}

// Helper: Format numbers with commas (e.g. 1,000,000)
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Helper: Get NX (Cash) safely
function getNxCredit() {
    try {
        return cm.getPlayer().getCashShop().getCash(1);
    } catch (e) {
        return 0;
    }
}