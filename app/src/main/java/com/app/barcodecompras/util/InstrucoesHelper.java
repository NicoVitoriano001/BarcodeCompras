package com.app.barcodecompras.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/**
 * Exibe instruções de como nomear um item no banco de dados.
 * Pode ser chamado a partir do drawer (todas as telas) ou do botão
 * de ajuda na tela AddItemBancoDados.
 */
public class InstrucoesHelper {

    private InstrucoesHelper() {
        // Classe utilitária - não instanciar
    }

    public static void mostrarInstrucoes(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("📋 Como Criar/Nomear Item no DB");

        // Conteúdo formatado: títulos em negrito + exemplos com recuo
        SpannableStringBuilder sb = new SpannableStringBuilder();

        appendBold(sb, "Siga a ordem abaixo para criar/nomear item no banco de dados:\n\n");

        appendBold(sb, "1. Marca/Fabricante\n");
        sb.append("       Ex.: Q-Boa, Sem Marca\n\n");

        appendBold(sb, "2. Versão/Sabor\n");
        sb.append("       Ex.: Tradicional/Trd, Top, Morango/Mor\n\n");

        appendBold(sb, "3. Embalagem/Material\n");
        sb.append("       Ex.: Garrafa/Gf, Lata/Lt, Vidro/Vd, Caixa/Cx\n\n");

        appendBold(sb, "4. Conteúdo/Peso/Volume\n");
        sb.append("       Ex.: Unidade/Un, 150g, 1000ml, Granel\n\n");

        sb.append("Exemplo completo:\n");
        appendBold(sb, "Q-Boa Trd Gf 1000ml");

        // TextView com scroll para telas pequenas
        TextView tv = new TextView(context);
        tv.setText(sb);
        tv.setTextSize(15);
        tv.setPadding(40, 24, 40, 24);

        ScrollView scroll = new ScrollView(context);
        scroll.addView(tv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        builder.setView(scroll);
        builder.setPositiveButton("Entendi", null);
        builder.show();
    }

    private static void appendBold(SpannableStringBuilder sb, String texto) {
        int start = sb.length();
        sb.append(texto);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
