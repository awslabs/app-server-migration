const codeFormatter = () => {
    const sourceElements = document.querySelectorAll("code:not(.skipBeautify)");
    for( let i = 0; i < sourceElements.length; i++ ) {
        const souceText = sourceElements[i].innerText;
        const formattedCode = js_beautify(souceText);
        sourceElements[i].innerHTML = formattedCode ;
    }
};
codeFormatter();