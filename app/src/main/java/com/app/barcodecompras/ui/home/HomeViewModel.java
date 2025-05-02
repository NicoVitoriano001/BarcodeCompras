package com.app.barcodecompras.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// HomeViewModel.java deve apenas gerenciar dados para o HomeFragment
public class HomeViewModel extends ViewModel {


    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("-");    //Aparece sob o botao scan
    }
    public LiveData<String> getText() {
        return mText;
    }

}