$(function(){

    //page2 下拉功能
    $(".page2>#area>.position>div").css("display","none")

    $(".page2>#area>.position").mouseenter(
        function(){
            $(this).children('.box').slideDown('fast');})

    $(".page2>#area>.position").mouseleave(
        function(){
            $(this).children('.box').stop(true,false);
            $(this).children('.box').slideUp('fast');})


    // $(".page3>#person a").each((index,value) => {
    //     // console.log($(value).children()[0].src)
    //     $(value).mouseenter(function(){
    //         // console.log($(this).children()[0].src)
    //         $('#persons>img').attr('src',"/image/person0"+index+".png")
    //     })

    //     $(value).mouseleave(function(){
    //         // console.log($(this).children()[0].src)
    //         $('#persons>img').attr('src',"/image/4gon.jpeg")
    //     })
    // })
    
    //page3 點選人物出現介紹
    $(".page3>#person a").each(function(index){  
        $(this).mouseenter(function(){
            $('#persons>img').attr('src',"/image/person0"+index+".png")
        })

        $(this).mouseleave(function(){
            $('#persons>img').attr('src',"/image/4gonNobg.png")
        })
    })


});