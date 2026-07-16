
/* Midori will become Ingrid ForgeMS style changer
	Change to any style available in the server.
*/
/*
 * Ingrid — ForgeMS Braidmaster
 * NPC ID: 9120101
 *
 * Universal cosmetic selector:
 * - Reads valid cosmetics directly from the server
 * - No coupons
 * - No NX or meso cost
 * - No level requirement
 * - No gender restriction
 *
 * Current options:
 * - Hairstyle
 * - Hair color
 * - Face / eyes
 * - Eye color
 * - Skin tone
 *
 * Sex change is intentionally excluded for now.
 */

var status = -1;
var category = -1;
var choices = null;

var HAIR_STYLE = 0;
var HAIR_COLOR = 1;
var FACE_STYLE = 2;
var EYE_COLOR = 3;
var SKIN_TONE = 4;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
        return;
    }

    if (mode === 0) {
        cm.dispose();
        return;
    }

    status++;

    if (status === 0) {
        showMainMenu();
        return;
    }

    if (status === 1) {
        category = selection;
        showCosmeticOptions();
        return;
    }

    if (status === 2) {
        applyCosmetic(selection);
        return;
    }

    cm.dispose();
}

function showMainMenu() {
    cm.sendSimple(
        "#eEvery braid, scar, and strand tells a story.#n\r\n\r\n" +
        "Whether you are returning from battle or preparing for the next " +
        "expedition, I can help you look the part.\r\n\r\n" +
        "What would you like to change?\r\n\r\n" +

        "#L" + HAIR_STYLE + "##bHairstyle#k#l\r\n" +
        "#L" + HAIR_COLOR + "##bHair Color#k#l\r\n" +
        "#L" + FACE_STYLE + "##bFace and Eyes#k#l\r\n" +
        "#L" + EYE_COLOR + "##bEye Color#k#l\r\n" +
        "#L" + SKIN_TONE + "##bSkin Tone#k#l"
    );
}

function showCosmeticOptions() {
    if (category === HAIR_STYLE) {
        choices = buildHairStyleChoices();

        if (choices.length === 0) {
            failNoOptions("hairstyles");
            return;
        }

        cm.sendStyle(
            "Choose the hairstyle that best suits you.",
            choices
        );
        return;
    }

    if (category === HAIR_COLOR) {
        choices = buildHairColorChoices();

        if (choices.length === 0) {
            failNoOptions("hair colors");
            return;
        }

        cm.sendStyle(
            "Choose a new hair color.",
            choices
        );
        return;
    }

    if (category === FACE_STYLE) {
        choices = toJavascriptArray(cm.getAllFaceIds());

        if (choices.length === 0) {
            failNoOptions("faces");
            return;
        }

        cm.sendStyle(
            "Choose the face and eyes that tell your story.",
            choices
        );
        return;
    }

    if (category === EYE_COLOR) {
        choices = buildEyeColorChoices();

        if (choices.length === 0) {
            failNoOptions("eye colors for your current face");
            return;
        }

        cm.sendStyle(
            "Choose a new eye color.",
            choices
        );
        return;
    }

    if (category === SKIN_TONE) {
        choices = toJavascriptArray(cm.getAllSkinIds());

        if (choices.length === 0) {
            failNoOptions("skin tones");
            return;
        }

        cm.sendStyle(
            "Choose your preferred skin tone.",
            choices
        );
        return;
    }

    cm.sendOk("That service is not currently available.");
    cm.dispose();
}

function applyCosmetic(selection) {
    if (choices === null ||
        selection < 0 ||
        selection >= choices.length) {

        cm.sendOk(
            "I could not identify that selection. Please speak with me again."
        );
        cm.dispose();
        return;
    }

    var selectedId = parseInt(choices[selection]);

    if (category === HAIR_STYLE || category === HAIR_COLOR) {
        cm.setHair(selectedId);
        cm.sendOk(
            "#eThere we are.#n\r\n\r\n" +
            "Your new hairstyle has been set."
        );
        cm.dispose();
        return;
    }

    if (category === FACE_STYLE || category === EYE_COLOR) {
        cm.setFace(selectedId);
        cm.sendOk(
            "#eA fine choice.#n\r\n\r\n" +
            "Your new appearance has been set."
        );
        cm.dispose();
        return;
    }

    if (category === SKIN_TONE) {
        cm.setSkin(selectedId);
        cm.sendOk(
            "#eFinished.#n\r\n\r\n" +
            "Your new skin tone has been set."
        );
        cm.dispose();
        return;
    }

    cm.sendOk("That service is not currently available.");
    cm.dispose();
}

/*
 * Preserves the player's current hair color while displaying every
 * valid hairstyle known by the server.
 */
function buildHairStyleChoices() {
    var allHairIds = toJavascriptArray(cm.getAllHairIds());
    var currentHair = cm.getPlayer().getHair();
    var currentColor = currentHair % 10;

    var results = [];
    var used = {};

    for (var i = 0; i < allHairIds.length; i++) {
        var baseHair = parseInt(allHairIds[i]);
        baseHair -= baseHair % 10;

        var candidate = baseHair + currentColor;

        if (!used[candidate]) {
            used[candidate] = true;
            results.push(candidate);
        }
    }

    results.sort(compareNumbers);
    return results;
}

/*
 * Builds complete hair IDs by combining the player's current hairstyle
 * with every valid hair-color value exposed by the server.
 */
function buildHairColorChoices() {
    var colorIds = toJavascriptArray(cm.getAllHairColorIds());
    var currentHair = cm.getPlayer().getHair();
    var baseHair = currentHair - (currentHair % 10);

    var results = [];
    var used = {};

    for (var i = 0; i < colorIds.length; i++) {
        var color = parseInt(colorIds[i]);
        var candidate = baseHair + color;

        if (!used[candidate]) {
            used[candidate] = true;
            results.push(candidate);
        }
    }

    results.sort(compareNumbers);
    return results;
}

/*
 * Eye-color variants in v83 face IDs generally share:
 * - the same 10,000-series family; and
 * - the same final two digits.
 *
 * This keeps the player's current face style while listing the valid
 * color variants present in the server's face data.
 */
function buildEyeColorChoices() {
    var allFaceIds = toJavascriptArray(cm.getAllFaceIds());
    var currentFace = cm.getPlayer().getFace();

    var currentFamily = Math.floor(currentFace / 10000);
    var currentStyle = currentFace % 100;

    var results = [];
    var used = {};

    for (var i = 0; i < allFaceIds.length; i++) {
        var faceId = parseInt(allFaceIds[i]);
        var faceFamily = Math.floor(faceId / 10000);
        var faceStyle = faceId % 100;

        if (faceFamily === currentFamily &&
            faceStyle === currentStyle &&
            !used[faceId]) {

            used[faceId] = true;
            results.push(faceId);
        }
    }

    /*
     * Fallback: if the current face uses an unusual custom numbering
     * pattern, show the current face rather than returning an empty menu.
     */
    if (results.length === 0) {
        results.push(currentFace);
    }

    results.sort(compareNumbers);
    return results;
}

function toJavascriptArray(javaArray) {
    var result = [];

    if (javaArray === null) {
        return result;
    }

    for (var i = 0; i < javaArray.length; i++) {
        result.push(parseInt(javaArray[i]));
    }

    return result;
}

function compareNumbers(a, b) {
    return a - b;
}

function failNoOptions(categoryName) {
    cm.sendOk(
        "I could not find any valid " + categoryName +
        " in the server data."
    );
    cm.dispose();
}