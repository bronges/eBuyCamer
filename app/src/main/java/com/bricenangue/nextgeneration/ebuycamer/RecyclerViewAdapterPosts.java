package com.bricenangue.nextgeneration.ebuycamer;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by bricenangue on 02/08/16.
 */
public class RecyclerViewAdapterPosts extends RecyclerView
        .Adapter<RecyclerViewAdapterPosts
        .PublicationViewHolders> {
    private static String LOG_TAG = "RecyclerViewAdapterPost";
    private ArrayList<Publication> mDataset;
    private MyRecyclerAdaptaterPostClickListener myClickListener;
    private Context context;
    private String [] currencyArray;




    public static class PublicationViewHolders extends RecyclerView.ViewHolder
     implements View.OnClickListener{
        ImageView postPicture,imageViewLocation;
        TextView titel, time, price, mylocation,isnegotiable;
        private View view;
        private MyRecyclerAdaptaterPostClickListener clickListener;


        public PublicationViewHolders(View itemView, MyRecyclerAdaptaterPostClickListener clickListener) {
            super(itemView);
            view=itemView;
            this.clickListener=clickListener;

            postPicture=(ImageView) itemView.findViewById(R.id.imageView_publicationFirstphoto);
            imageViewLocation=(ImageView) itemView.findViewById(R.id.imageView_publicationLocation);

            isnegotiable=(TextView)itemView.findViewById(R.id.textView_publication_is_negotiable);
            titel=(TextView) itemView.findViewById(R.id.textView_publication_title);
            time=(TextView) itemView.findViewById(R.id.textView_publication_time);
            price=(TextView) itemView.findViewById(R.id.textView_publication_price);
            mylocation=(TextView) itemView.findViewById(R.id.textView_publication_locatiomn);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            clickListener.onItemClick(getAdapterPosition(),view);
        }
    }


    public void setOnPostClickListener(MyRecyclerAdaptaterPostClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    public RecyclerViewAdapterPosts(Context context, ArrayList<Publication> myDataset
            , MyRecyclerAdaptaterPostClickListener myClickListener) {
        this.context=context;
        mDataset = myDataset;
        this.myClickListener=myClickListener;
        currencyArray=context.getResources().getStringArray(R.array.currency);

    }

    @Override
    public PublicationViewHolders onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publication, parent, false);

        PublicationViewHolders dataObjectHolder = new PublicationViewHolders(view,myClickListener);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(final PublicationViewHolders viewHolder, final int position) {
       final Publication model=mDataset.get(position);

        if(model.getPrivateContent().isNegotiable()){
            viewHolder.isnegotiable.setText(context.getString(R.string.text_is_not_negotiable));
        }else {
            viewHolder.isnegotiable.setText("");
        }
        viewHolder.titel.setText(model.getPrivateContent().getTitle());
        viewHolder.mylocation.setText(model.getPrivateContent().getLocation().getName());
       if(model.getPrivateContent().getFirstPicture()!=null){
           byte[] decodedString = Base64.decode(model.getPrivateContent().getFirstPicture(), Base64.DEFAULT);
           Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

           viewHolder.postPicture.setImageBitmap(decodedByte);

        }else {
           if(model.getPrivateContent().getPublictionPhotos()!=null){
               Picasso.with(context).load(model.getPrivateContent().getPublictionPhotos().get(0).getUri())
                       .networkPolicy(NetworkPolicy.OFFLINE)
                       .fit().centerInside()
                       .into(viewHolder.postPicture, new Callback() {
                           @Override
                           public void onSuccess() {

                           }

                           @Override
                           public void onError() {
                               Picasso.with(context).load(model.getPrivateContent()
                                       .getPublictionPhotos().get(0).getUri())
                                       .fit().centerInside().into(viewHolder.postPicture);

                           }
                       });

           }else {
               viewHolder.postPicture.setImageDrawable(context.getResources().getDrawable(R.mipmap.ic_launcher));
           }
        }





        DecimalFormat decFmt = new DecimalFormat("#,###.##", DecimalFormatSymbols.getInstance(Locale.GERMAN));
        decFmt.setMaximumFractionDigits(2);
        decFmt.setMinimumFractionDigits(2);

        String p=model.getPrivateContent().getPrice();
        BigDecimal amt = new BigDecimal(p);
        String preValue = decFmt.format(amt);

        if(p.equals("0")){
            viewHolder.price.setText(context.getString(R.string.check_box_create_post_hint_is_for_free));
        }else {
            if(currencyArray!=null){
                viewHolder.price.setText(preValue + " " + currencyArray[getCurrencyPosition(model.getPrivateContent().getCurrency())]);

            }else {
                viewHolder.price.setText(preValue + " " + model.getPrivateContent().getCurrency());

            }
        }



        Date date = new Date(model.getPrivateContent().getTimeofCreation());
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        String dateFormatted = formatter.format(date);

        CheckTimeStamp checkTimeStamp= new CheckTimeStamp(context,model.getPrivateContent().getTimeofCreation());
        viewHolder.time.setText(checkTimeStamp.checktime());



    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface MyRecyclerAdaptaterPostClickListener {
        public void onItemClick(int position, View v);
        public void onLongClick(int position, View v);
    }


    private int getCurrencyPosition(String currency){
        if(currency.equals(context.getString(R.string.currency_xaf))
                || currency.equals("F CFA") || currency.equals("XAF")){
            return 0;
        }
        return 0;

    }
}
