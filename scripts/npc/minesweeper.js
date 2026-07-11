const START = 4000000;
const BOMB = 4000020;
const HIDDEN = 4000022;

const WIDTH = 6;
const HEIGHT = 6;
const BOMBS = 3;

class Space {

  constructor() {
    this.content = START;
    this.revealed = false;
  }
}

const field = [];
let revealed = 0;

function start() {
  generateField();
  action(1, 0, 0);
}

function action(m, t, s) {
  if (m !== 1) {
    cm.dispose();
    return;
  }

  if (t === 4) {
    let selected = field[Math.floor(s / WIDTH)][s % WIDTH].content;
    if (selected === BOMB) {
      endMessage("Game over:");
      return;
    } else {
      field[Math.floor(s / WIDTH)][s % WIDTH].revealed = true;
      revealed++;
    }
  }

  if (revealed === WIDTH * HEIGHT - BOMBS) {
    endMessage("You won!!");
    return;
  }

  let str = "Click to die:\r\n\r\n";
  for (let y = 0; y < HEIGHT; y++) {
    for (let x = 0; x < WIDTH; x++) {
      str += `#L${(y * WIDTH) + x}##i${field[y][x].revealed ? field[y][x].content : HIDDEN}##l `;
    }
    str += "\r\n";
  }
  cm.sendSimple(str);
}

function endMessage(msg) {
  let str = msg + "\r\n\r\n";
  for (let y = 0; y < HEIGHT; y++) {
    for (let x = 0; x < WIDTH; x++) {
      str += `#i${field[y][x].content}# `;
    }
    str += "\r\n";
  }
  cm.sendOk(str);
  cm.dispose();
}

function generateField() {
  for (let y = 0; y < HEIGHT; y++) {
    field[y] = [];
    for (let x = 0; x < WIDTH; x++) {
      field[y][x] = new Space();
    }
  }

  for (let f = 0; f < BOMBS; f++) {
    let randY = -1;
    let randX = -1;
    while (randY === -1 || field[randY][randX].content === BOMB) {
      randY = Math.floor(Math.random() * HEIGHT);
      randX = Math.floor(Math.random() * WIDTH);
    }

    field[randY][randX].content = BOMB;

    // Update neighboring cells
    for (let i = -1; i <= 1; i++) {
      for (let j = -1; j <= 1; j++) {
        const newRow = randY + i;
        const newCol = randX + j;

        // Check if the neighboring cell is within the grid boundaries
        if (newRow >= 0 && newRow < HEIGHT && newCol >= 0 && newCol < WIDTH) {
          if (field[newRow][newCol].content !== BOMB) {
            field[newRow][newCol].content++;
          }
        }
      }
    }
  }
}