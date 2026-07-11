let pairs;

const START = 4000000;
const HIDDEN = 4000300;

class Card {
  constructor() {
    this.item = -1;
    this.show = false;
  }
}

const cards = [];
let first = -1;
let second = -1;
let correct = 0;
let fails = 0;

function start() {
  cm.sendGetNumber("How many pairs do you want to play with?", 10, 2, 150);
}

function action(m, t, s) {
  if (m !== 1) {
    cm.dispose();
    return;
  }

  if (t === 3) {
    pairs = s;
    generateBoard();
  }

  if (t === 4) {
    if (first === -1) {
      first = s;
    } else if (second === -1 && first !== s) {
      second = s;
      if (cards[first].item === cards[second].item) {
        cards[first].show = true;
        cards[second].show = true;
        correct++;
      } else {
        fails++;
      }
    } else {
      first = s;
      second = -1;
    }
  }

  if (correct === pairs) {
    cm.sendOk(`Game complete!\r\nYou failed #e#r${fails}#k#n times, there were #e#b${pairs}#k#n pairs.`);
    cm.dispose();
    return;
  }

  let str = `Click ${first === -1 ? 'first' : 'second'} item (You've failed ${fails} times):\r\n\r\n`;
  for (let i = 0; i < cards.length; i++) {
    str += `#L${i}##i${i === first || i === second || cards[i].show ? cards[i].item : HIDDEN}##l `;
    if (i % 5 === 4) str += '\r\n';
  }
  cm.sendSimple(str);
}

function generateBoard() {
  for (let i = 0; i < pairs * 2; i++) {
    cards[i] = new Card();
    cards[i].item = START + Math.floor(i / 2);
  }
  shuffle(cards);
}

// https://stackoverflow.com/a/2450976
function shuffle(array) {
  let currentIndex = array.length,  randomIndex;

  // While there remain elements to shuffle.
  while (currentIndex > 0) {

    // Pick a remaining element.
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex--;

    // And swap it with the current element.
    [array[currentIndex], array[randomIndex]] = [
      array[randomIndex], array[currentIndex]];
  }
}