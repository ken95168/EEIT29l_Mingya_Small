function option(){

    $(".q input").each((index,element) => {
        if($(element).is(":checked")){
            console.log($(element).val());
            
        }
    });

}