function enter(pi) {
    if (pi.isQuestCompleted(31151)) {
    pi.openNpc(2143000, '2143000');
    return true;
    } else {
    pi.dropMessage(5, "You must complete the Dream Key quest in order to enter the Hallowed Grounds.");
    return false;
    }
  }