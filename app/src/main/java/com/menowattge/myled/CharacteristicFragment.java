package com.menowattge.myled;

import android.app.Fragment;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CharacteristicFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CharacteristicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CharacteristicFragment extends Fragment {



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    //chiude il fragment
    //connect/disconnect
    protected static ToggleButton disConnect;
    private OnFragmentInteractionListener mListener;
    protected Spinner spinnerDue ;
    protected ArrayAdapter adapter;
    protected Button invia;

    ProgressBar progressBarhorizontal;

    protected String selezionaProgramma = "Seleziona un Programma";
    protected String programmaUno = "Programma 1:23M2";
    protected String programmaDue ="Programma 2:23M3";
    protected String programmaTre = "Programma 3:22M2";
    protected String programmaQuattro="Programma 4:22M3";
    protected String programmaCinque="Programma 5:ERP";
    protected String programmaSei="Programma 6:EMX";
    protected String programmaSette="Programma 7:23EMP";
    protected String programmaOtto="Programma 8:22EMP";
    protected String programmaNove="Programma 9:23ERP";
    protected String programmaDieci="Programma 10:22ERP";
    protected String programmaUndici="Programma 11:23M2S2";
    protected String programmaDodici="Programma 12:23M3S2";
    protected String programmaTredici="Programma 13:22M2S2";
    protected String programmaQuattordici="Programma 14:22M3S2";
    protected String programmaQuindici="Programma 15:LSM2";
    protected String programmaSedici="Programma 16:LSM3";
    protected String programmaDiciassette="Programma 17:LSM2S2";
    protected String programmaDiciotto="Programma 18:LSM3S2";
    protected String programmaDiciannove="Programma 19:R400";

    //dichiarato per essere instanziato nel main
    public void showUuid(UUID a){
        Toast.makeText(getActivity(),"->Device Connesso<-"+"\n  UUID : "+a,Toast.LENGTH_LONG).show();
        Toast.makeText(getActivity(),"seleziona un programma",Toast.LENGTH_LONG).show();
        //spinner enabled ad avvenuta connessione
        spinnerDue.setEnabled(true);

    }

    public CharacteristicFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CharacteristicFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CharacteristicFragment newInstance(String param1, String param2) {
        CharacteristicFragment fragment = new CharacteristicFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

     View view = null;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {


        view = inflater.inflate(R.layout.fragment_characteristic, container, false);

        invia = (Button)view.findViewById(R.id.buttonInvia);
        disConnect = (ToggleButton) view.findViewById(R.id.toggleButton);
        spinnerDue = (Spinner)view.findViewById(R.id.spinner2);



        adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_spinner_dropdown_item,
                new String[]{//selezionaProgramma,
                        programmaUno,programmaDue,programmaTre,programmaQuattro,programmaCinque,programmaSei,
                        programmaSette, programmaOtto,programmaNove,programmaDieci,programmaUndici,programmaDodici,
                        programmaTredici, programmaQuattordici,programmaQuindici,programmaSedici,programmaDiciassette,
                        programmaDiciotto,programmaDiciannove});


        spinnerDue.setAdapter(adapter);


        spinnerDue.setEnabled(false);
        invia.setEnabled(false);


        disConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BluetoothGatt btGatt = ((MainActivity) getActivity()).connectToDevice(((MainActivity) getActivity()).devProva);
                if(disConnect.isChecked()) {
                    //mi connetto
                    ((MainActivity) getActivity()).connectToDevice(((MainActivity) getActivity()).devProva);

                }
                else if (!disConnect.isChecked()){
                    //mi disconnetto
                    ((MainActivity) getActivity()).discConnectToDevice(btGatt);

                    //rimuove il fragment dallo stack come quando si preme il tasto "back"
                    getFragmentManager().popBackStack();
                    //deve tornare visibile
                    MainActivity.startScanningButton.setVisibility(View.VISIBLE);
                    //riesegue la scansione per ripopolare la lista
                    ((MainActivity)getActivity()).startScanning();
                }
            }
        });


        return view;
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
