(function () {
    var toggle = document.querySelector('[data-sidebar-toggle]');
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.querySelector('.sidebar-overlay');
    if (!toggle || !sidebar) {
        return;
    }
    function setOpen(open) {
        sidebar.classList.toggle('is-open', open);
        if (overlay) {
            overlay.classList.toggle('is-visible', open);
        }
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        toggle.setAttribute('aria-label', open ? '关闭菜单' : '打开菜单');
    }
    toggle.addEventListener('click', function () {
        setOpen(!sidebar.classList.contains('is-open'));
    });
    if (overlay) {
        overlay.addEventListener('click', function () {
            setOpen(false);
        });
    }
    sidebar.querySelectorAll('a').forEach(function (link) {
        link.addEventListener('click', function () {
            setOpen(false);
        });
    });
})();
