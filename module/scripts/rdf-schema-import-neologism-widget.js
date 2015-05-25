function ImportNeologismWidget(manager) {
    this._prefixesManager = manager;
}

ImportNeologismWidget.prototype.show = function (msg, def_prefix, onDone) {
    var self = this;
    var frame = DialogSystem.createDialog();

    frame.width("450px");

    var html = $(DOM.loadHTML("rdf-extension", "scripts/import-neologism-widget.html"));

    var header = $('<div></div>').addClass("dialog-header").text("Import Neoologism Repository").appendTo(frame);
    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").append(html).appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

    self._elmts = DOM.bind(html);
    if (msg) {
        self._elmts.message.addClass('message').html(msg);
    }

    if (def_prefix) {
        self._elmts.prefix.val(def_prefix);
        self.suggestUri(def_prefix);
    }

    var importRepository = function (onDone) {
        var uri = self._elmts.repo_url.val();
        var dismissBusy = DialogSystem.showBusy('Trying to import vocabulary from ' + uri);

        $.post("command/rdf-extension/import-neologism-repository", {
            uri: uri,
            "fetch-url": uri,
            project: theProject.id
        }, function (data) {
            if (data.code === "error") {
                alert('Error:' + data.message)
            } else {
                alert(data.message);
                DialogSystem.dismissUntil(level - 1);
                if (onDone) {
                    onDone(data.vocabs);
                }
            }
            dismissBusy();
        });
    };

    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function () {
        importRepository(onDone);
    }).appendTo(footer);

    $('<button></button>').addClass('button').text("Cancel").click(function () {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);

    var level = DialogSystem.showDialog(frame);
};


