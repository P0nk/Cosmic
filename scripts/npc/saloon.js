// #!js
var Saloon = Java.type('client.Saloon');

var status = -1;
var selected;
var selectedStyle;

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose(); 
    } else {
        if (mode == 1) {
            status++;
        } else {
            status--;
            cm.dispose();
        }

		if (status == 0) {
			cm.sendSimple("Hi there, I'm the manager of the Saloon. You can save your appearance's here. \r\n#L0#Change Appearance#l\r\n#L1#Save Appearance#l");
		
		} else if (status == 1) {
			selected = selection;

			if (selected == 0) {
				var styles = cm.getPlayer().getStyles();

				// FIX: check if empty
				if (styles == null || styles.size() == 0) {
					cm.sendOk("You don't have any saved appearances.");
					cm.dispose();
					return;
				}

				var text = "";
				for (var i = 0; i < styles.size(); i++) {
					text += "#L" + i + "# #b" + styles.get(i).getLoadoutName() + " #l\r\n";
				}
				cm.sendSimple("Select the appearance: \r\n" + text);

			} else if (selected == 1) {
				cm.sendGetText("What would you like to name your outfit? (Note: The appearance can be up to 20 characters long.)");
			}
			
        } else if (status == 2) {

			if (selected == 0) {
				selectedStyle = selection;

				var appearance = cm.getPlayer().getStyles().get(selectedStyle);
				if (appearance != null) {

					cm.setHair(appearance.getHair());
					cm.setFace(appearance.getFace());
					cm.setSkin(appearance.getSkin());
					cm.sendNext("Your new look has been applied!");	
				}

			} else if (selected == 1) {
				var name = cm.getText();

				if (sanitize(name)) {
					return;
				}
			
				var hair = cm.getPlayer().getHair();
				var face = cm.getPlayer().getFace();
				var skin = cm.getPlayer().getSkinColor().ordinal();
					
				var sal = new Saloon(name, hair, face, skin);
			
				cm.getPlayer().getStyles().add(sal);
				cm.sendOk("Your appearance has been saved as #b" + name + "#n.");
			}
			
			cm.dispose();
		}
    }
}

function sanitize(name) {

	if (name.length > 20) {
		cm.sendNext("The name is too long for the appearance.");
		cm.dispose();
		return true;
	}

	for (var i = 0; i < name.length; i++) {
		var ch = name.charAt(i);
		var code = ch.charCodeAt(0);

		var isLetter = (code >= 65 && code <= 90) || (code >= 97 && code <= 122);
		var isDigit = (code >= 48 && code <= 57);
		var isSpace = (code === 32);

		if (!(isLetter || isDigit || isSpace)) {
			cm.sendNext("You cannot enter special characters.");
			cm.dispose();
			return true;
		}
	}

	return false;
}