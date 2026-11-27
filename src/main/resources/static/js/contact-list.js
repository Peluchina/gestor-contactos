(() => {
    const searchInput = document.querySelector("[data-contact-search]");
    const commercialFilter = document.querySelector("[data-commercial-filter]");
    const rows = Array.from(document.querySelectorAll("[data-contact-row]"));
    const emptyFilterRow = document.querySelector("[data-filter-empty]");
    const requestedType = new URLSearchParams(window.location.search).get("commercialType");

    if (!rows.length) {
        return;
    }

    if (commercialFilter && ["CLIENTE", "PROVEEDOR"].includes(requestedType)) {
        commercialFilter.value = requestedType;
    }

    const filterRows = () => {
        const searchText = (searchInput?.value || "").trim().toLocaleLowerCase("es");
        const selectedType = commercialFilter?.value || "";
        let visibleRows = 0;

        rows.forEach((row) => {
            const matchesText = row.textContent.toLocaleLowerCase("es").includes(searchText);
            const matchesType = !selectedType || (row.dataset.commercialTypes || "").includes(selectedType);
            row.hidden = !(matchesText && matchesType);
            if (!row.hidden) {
                visibleRows += 1;
            }
        });

        if (emptyFilterRow) {
            emptyFilterRow.hidden = visibleRows > 0;
        }
    };

    searchInput?.addEventListener("input", filterRows);
    commercialFilter?.addEventListener("change", filterRows);
    filterRows();
})();
