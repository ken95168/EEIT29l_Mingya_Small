function licenseDownload() {
    html2canvas(document.querySelector("#allpic")).then(function (canvas) {
        a = document.createElement('a');
        a.href = canvas.toDataURL("image/jpeg", 0.92).replace("image/jpeg", "image/octet-stream");
        a.download = 'image.jpg';
        a.click();
    });
}