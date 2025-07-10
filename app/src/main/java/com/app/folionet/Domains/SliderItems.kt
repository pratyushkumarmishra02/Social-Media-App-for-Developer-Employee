package com.app.folionet.Domains

class SliderItems {
    var projectTitle: String = ""
    var projectDesc: String = ""
    var projectTech: String = ""
    var imageUrl: String = ""

    constructor(title: String?, desc: String?, tech: String?, image: String?) {
        this.projectTitle = title!!
        this.projectDesc = desc!!
        this.imageUrl = image!!
        this.projectTech = tech!!


    }


}