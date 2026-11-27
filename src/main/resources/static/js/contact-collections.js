document.querySelectorAll("[data-add-row]").forEach((button) => {
    const collection = document.getElementById(button.dataset.addRow);
    if (!collection) {
        return;
    }

    const group = collection.dataset.collection;
    const rows = () => Array.from(collection.querySelectorAll("[data-row]"));

    const reindex = () => {
        rows().forEach((row, index) => {
            row.querySelectorAll("[name]").forEach((field) => {
                field.name = field.name.replace(new RegExp(`${group}\\[\\d+\\]`), `${group}[${index}]`);
                if (field.id) {
                    field.id = field.id.replace(/-\d+$/, `-${index}`);
                }
            });
            row.querySelectorAll("label[for]").forEach((label) => {
                label.htmlFor = label.htmlFor.replace(/-\d+$/, `-${index}`);
            });
            const legend = row.querySelector("legend");
            if (legend) {
                legend.textContent = group === "phones" ? `Teléfono ${index + 1}` : `Correo ${index + 1}`;
            }
        });
    };

    button.addEventListener("click", () => {
        const lastRow = rows().at(-1);
        if (!lastRow) {
            return;
        }
        const newRow = lastRow.cloneNode(true);
        newRow.querySelectorAll("input").forEach((input) => input.value = "");
        newRow.querySelectorAll("select").forEach((select) => select.selectedIndex = 0);
        collection.appendChild(newRow);
        reindex();
    });

    collection.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-remove-row]");
        if (removeButton && rows().length > 1) {
            removeButton.closest("[data-row]").remove();
            reindex();
        }
    });
});
