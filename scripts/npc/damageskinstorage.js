const PAGE_SIZE = 10;  // Number of skins per page
let currentPage = 1;
let totalPages = 1;
let selectedSkinId = null;
let savedSkins = [];

function start() {
    // Fetch saved damage skins from Java
    savedSkins = cm.getPlayer().getSavedDamageSkins();  // Assuming this calls the Java method

    if (savedSkins.length === 0) {
        cm.sendOk("You have no saved damage skins.");
        cm.dispose();
        return;
    }

    // Calculate total pages
    totalPages = Math.ceil(savedSkins.length / PAGE_SIZE);

    // Show the page selection menu first
    showPageSelection();
}

function showPageSelection() {
    let str = "Select a page to view saved damage skins:\r\n";
    str += "\r\n";  // Add one extra line break for spacing

    // Display page numbers for selection
    for (let i = 1; i <= totalPages; i++) {
        str += "#L" + i + "##rPage " + i + "#l   #k";  // Each page is a selectable link
        if (i % 5 === 0) {
            str += "\r\n";  // Add a new line after every 5 pages
        }
    }

    str += "\r\n#L100000#Exit#l ";  // Option to exit

    cm.sendSimple(str);
}

function showPage(page) {
    let str = "Select a saved damage skin (Page " + page + " of " + totalPages + "):\r\n";
    str += "\r\n";  // Add one extra line break for spacing

    const start = (page - 1) * PAGE_SIZE;
    const end = Math.min(start + PAGE_SIZE, savedSkins.length);

    // Display saved skins for the current page
    for (let i = start; i < end; i++) {
        const skinId = savedSkins[i];
        str += "#L" + (200000 + skinId) + "##fEffect/DamageSkin.img/" + skinId + "/NoRed0/7##l   ";  // Offset skin IDs to avoid conflict
        if ((i + 1) % 5 === 0) {
            str += "\r\n";  // Add a new line after every 5 skins
        }
    }

    str += "\r\n";  // Space between skins and navigation options
    str += "#L100001#Previous Page#l ";
    str += "#L100002#Next Page#l ";
    str += "\r\n#L100000#Exit#l ";  // Option to exit

    cm.sendSimple(str);
}

function action(mode, type, selection) {
    if (mode === 1) {
        if (selection === 100000) {  // Exit
            cm.dispose();
        } else if (selection >= 1 && selection <= totalPages) {  // Selected a page number
            currentPage = selection;
            showPage(currentPage);  // Show skins for the selected page
        } else if (selection === 100001) {  // Previous page
            if (currentPage > 1) {
                currentPage--;
            }
            showPage(currentPage);
        } else if (selection === 100002) {  // Next page
            if (currentPage < totalPages) {
                currentPage++;
            }
            showPage(currentPage);
        } else if (selection >= 200000) {  // Apply selected saved skin
            selectedSkinId = selection - 200000;  // Remove offset to get original skin ID

            // Apply the selected skin
            cm.getPlayer().setDamageSkin(selectedSkinId, true);
            cm.sendOk("Your damage skin has been changed to Skin ID: " + selectedSkinId + ".");
            cm.dispose();
        }
    } else {
        cm.dispose();
    }
}
