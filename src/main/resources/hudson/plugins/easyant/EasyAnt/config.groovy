package hudson.plugins.easyant.EasyAnt;
f=namespace(lib.FormTagLib)

if (descriptor.installations.length != 0) {
    f.entry(title:_("Ant Version")) {
        select(class:"setting-input",name:"easyAnt.easyAntName") {
            option(value:"(Default)", _("Default"))
            descriptor.installations.each {
                f.option(selected:it.name==instance?.easyAnt?.name, value:it.name, it.name)
            }
        }
    }
}

f.entry(title:_("Targets"),field:"targets") {
    f.expandableTextbox()
}

f.advanced {
    f.entry(title:_("Build Module"),field:"buildModule") {
        f.expandableTextbox()
    }
    f.entry(title:_("Build File"),field:"buildFile") {
        f.expandableTextbox()
    }
    f.entry(title:_("Properties"),field:"properties") {
        f.expandableTextbox()
    }
    f.entry(title:_("Java Options"),field:"easyAntOpts") {
        f.expandableTextbox()
    }
}