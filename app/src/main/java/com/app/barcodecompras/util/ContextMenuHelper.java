package com.app.barcodecompras.util;

import android.view.View;
import android.widget.PopupMenu;

import com.app.barcodecompras.Compra;
import com.app.barcodecompras.R;

import java.lang.reflect.Method;
public class ContextMenuHelper {

    public static void showContextMenu(View anchor, Compra compra,
                                       Runnable onEdit, Runnable onDelete,
                                       Runnable onClone, Runnable onSearch) { // ← adicionado onSearch
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.inflate(R.menu.context_menu);

        // FORÇAR A EXIBIÇÃO DOS ÍCONES
        try {
            java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceIcons.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_editar) {
                if (onEdit != null) onEdit.run();
                return true;
            } else if (id == R.id.menu_deletar) {
                if (onDelete != null) onDelete.run();
                return true;
            } else if (id == R.id.menu_clonar) {
                if (onClone != null) onClone.run();
                return true;
            } else if (id == R.id.menu_pesquisar) { // ← novo item
                if (onSearch != null) onSearch.run();
                return true;
            }
            return false;
        });
        popup.show();
    }
}