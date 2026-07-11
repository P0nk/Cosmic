const file = Java.type('java.nio.file.Files');
const path = Java.type('java.nio.file.Paths');

function getJson(filePath) {
    filePath = "scripts/json/" + filePath;
    const jsonContent = file.readString(path.get(filePath))
    return JSON.parse(jsonContent);
}

function convert(arr) {
    return JSON.stringify(arr);
}

const module = {
    getJson: getJson,
    convert: convert
}
this.module = module;