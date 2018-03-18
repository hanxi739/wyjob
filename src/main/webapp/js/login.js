$(document).ready(function(){
  $(".find-btn").click(function(){
    $(".find-btn").addClass("focus-btn");
    $(".recruit-btn").removeClass("focus-btn");
    $("#find-form").removeClass("none");
    $("#recruit-form").addClass("none");
  });
  $(".recruit-btn").click(function(){
    $(".find-btn").removeClass("focus-btn");
    $(".recruit-btn").addClass("focus-btn");
    $("#find-form").addClass("none");
    $("#recruit-form").removeClass("none");
  });
  $(".login-btn").click(function(){
  	$("#find-form").addClass("none");
    $("#recruit-form").addClass("none");
    $("#login-form").removeClass("none");
  });
  $("#register-btn").click(function(){
  	$(".find-btn").addClass("focus-btn");
    $(".recruit-btn").removeClass("focus-btn");
  	$("#find-form").removeClass("none");
    $("#recruit-form").addClass("none");
    $("#login-form").addClass("none");
  });
  $(".submit-btn").click(function(){
    if ($("#name").val() == '0') {
      $(location).attr('href', '../html/job_search.html');
    }
    if ($("#name").val() == '1') {
      $(location).attr('href', '../html/company_info.html');
    }
  });
});