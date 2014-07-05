$(window).bind("load", function() {
$("div#image-carousel").slideViewerPro({
	galBorderWidth: 6, // the border width around the main images
	thumbsTopMargin: 3, // the distance that separates the thumbnails and the buttons from the main images
	thumbsRightMargin: 1, // the distance of each thumnail respect to the next one
	thumbsBorderWidth: 3, // the border width of each thumbnail. Note that the border in reality is above, not around
	buttonsWidth: 20, // the width of the prev/next buttons that commands the thumbnails
	galBorderColor: "transparent", // the border color around the main images
	thumbsBorderColor: "#d8d8d8", // the border color of the thumbnails but not the current one
	thumbsActiveBorderColor: "#0074CC", // the border color of the current thumbnail
	buttonsTextColor: "#ff0000", //the color of the optional text in leftButtonInner/rightButtonInner
	thumbsBorderOpacity: 1.0, // could be 0, 0.1 up to 1.0
	thumbsActiveBorderOpacity: 1.0, // could be 0, 0.1 up to 1.0
	easeTime: 0, // the time it takes a slide to move to its position
	asTimer: 4000, // if autoslide is true, this is the interval between each slide
	thumbs: 3, // the number of visible thumbnails
	thumbsPercentReduction: 25, // the percentual reduction of the thumbnails in relation to the original
	thumbsVis: true, // with this option set to false, the whole UI (thumbs and buttons) are not visible
	leftButtonInner:  "<i class='icon-chevron-left'/>", //could be an image "<img src='images/larw.gif' />" or an escaped char as "&larr";
	rightButtonInner: "<i class='icon-chevron-right'/>", //could be an image or an escaped char as "&rarr";
	autoslide: false, // by default the slider do not slides automatically. When set to true REQUIRES the jquery.timers plugin
	typo: false, // the typographic info of each slide. When set to true, the ALT tag content is displayed
	typoFullOpacity: 0.9, // the opacity for typographic info. 1 means fully visible.
	shuffle: false // the LI items can be shuffled (randomly mixed) when shuffle is true 

});
}); 	

	