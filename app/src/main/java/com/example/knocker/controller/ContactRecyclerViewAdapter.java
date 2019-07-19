package com.example.knocker.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.knocker.R;
import com.example.knocker.controller.activity.EditContactActivity;
import com.example.knocker.controller.activity.MainActivity;
import com.example.knocker.controller.activity.group.GroupActivity;
import com.example.knocker.model.ContactGesture;
import com.example.knocker.model.ContactList;
import com.example.knocker.model.ModelDB.ContactDB;
import com.example.knocker.model.ModelDB.ContactWithAllInformation;
import com.example.knocker.model.ModelDB.GroupDB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.sql.DriverManager.println;

/**
 * La Classe qui permet de remplir la RecyclerView avec les bon éléments
 *
 * @author Florian Striebel, Kenzy Suon, Ryan Granet
 */
public class ContactRecyclerViewAdapter extends RecyclerView.Adapter<ContactRecyclerViewAdapter.ContactViewHolder> {
    private List<ContactWithAllInformation> listContacts;
    private LayoutInflater layoutInflater;
    private Context context;
    private Integer len;
    private View view;
    private ContactList gestionnaireContacts;

    public ArrayList<ContactWithAllInformation> getListOfItemSelected() {
        return listOfItemSelected;
    }

    private ArrayList<ContactWithAllInformation> listOfItemSelected = new ArrayList<>();

    private final int PERMISSION_CALL_RESULT = 1;
    private String numberForPermission = "";

    public ContactRecyclerViewAdapter(Context context, List<ContactWithAllInformation> listContacts, Integer len, ContactList gestionnaireContacts) {
        this.context = context;
        this.listContacts = listContacts;
        this.len = len;
        this.layoutInflater = LayoutInflater.from(context);
        this.gestionnaireContacts = gestionnaireContacts;
    }

    public ContactWithAllInformation getItem(int position) {
        return listContacts.get(position);
    }

    public void setGestionnairecontact(ContactList gestionnaireContact) {
        this.gestionnaireContacts = gestionnaireContact;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (len == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_contact_item_layout_smaller, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_contact_item_layout, parent, false);
        }

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactViewHolder holder, final int position) {
        final ContactDB contact = getItem(position).getContactDB();
        assert contact != null;

        if (len == 0) {
            if (contact.getContactPriority() == 0) {
                holder.contactFirstNameView.setTextColor(context.getResources().getColor(R.color.priorityZeroColor));
            } else if (contact.getContactPriority() == 2) {
                holder.contactFirstNameView.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
            }
        } else {
            if (contact.getContactPriority() == 0) {
                holder.contactRoundedImageView.setBorderColor(context.getResources().getColor(R.color.priorityZeroColor));
            } else if (contact.getContactPriority() == 1) {
                holder.contactRoundedImageView.setBorderColor(context.getResources().getColor(R.color.lightColor));
            } else if (contact.getContactPriority() == 2) {
                holder.contactRoundedImageView.setBorderColor(context.getResources().getColor(R.color.priorityTwoColor));
            }

        }
        if (len == 1) {
            if (!contact.getProfilePicture64().equals("")) {
                Bitmap bitmap = base64ToBitmap(contact.getProfilePicture64());
                holder.contactRoundedImageView.setImageBitmap(bitmap);
            } else {
                System.out.println(contact.getProfilePicture());
                holder.contactRoundedImageView.setImageResource(randomDefaultImage(contact.getProfilePicture()));
            }
        }

        String contactName = contact.getFirstName() + " " + contact.getLastName();

        if (len == 0) {
            if (contactName.length() > 14) {

                Spannable spanFistName = new SpannableString(contactName);
                spanFistName.setSpan(new RelativeSizeSpan(1.0f), 0, contactName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.contactFirstNameView.setText(spanFistName);

                contactName = contact.getFirstName() + " " + contact.getLastName();
                contactName = contactName.substring(0, 13) + "..";
            }
        } else {
            if (contactName.length() > 25) {

                Spannable spanFistName = new SpannableString(contactName);
                spanFistName.setSpan(new RelativeSizeSpan(1.0f), 0, contactName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.contactFirstNameView.setText(spanFistName);

                contactName = contact.getFirstName() + " " + contact.getLastName();
                contactName = contactName.substring(0, 24) + "..";
            }
        }


        holder.contactFirstNameView.setText(contactName);
        String group= "";
        GroupDB firstGroup=getItem(position).getFirstGroup(context);
        if(context instanceof GroupActivity){
            holder.groupWordingConstraint.setVisibility(View.VISIBLE);
            if(len==0){
                holder.contactRoundedImageView.setVisibility(View.INVISIBLE);
            }
        }
        if(firstGroup==null){
            System.out.println("no group"+contact.getFirstName()+" "+contact.getLastName());
            Drawable roundedLayout= context.getDrawable(R.drawable.rounded_rectangle_group);
            roundedLayout.setColorFilter(Color.parseColor("#f0f0f0"), PorterDuff.Mode.MULTIPLY);
            holder.groupWordingConstraint.setBackground(roundedLayout);
        }else{
            System.out.println("have group");
            group = firstGroup.getName();
            Drawable roundedLayout = context.getDrawable(R.drawable.rounded_rectangle_group);
            assert roundedLayout != null;
            roundedLayout.setColorFilter(firstGroup.randomColorGroup(this.context), PorterDuff.Mode.MULTIPLY);
            holder.groupWordingConstraint.setBackground(roundedLayout);
        }
        if (len == 0) {
            if (group.length() > 6) {
                group = group.substring(0, 6).concat("..");
            }
        } else {
            if (group.length() > 9) {
                group = group.substring(0, 9).concat("..");
            }
        }
        if(holder.groupWordingTv != null){
            holder.groupWordingTv.setText(group);
        }
        View.OnClickListener listener = new View.OnClickListener() {
            @SuppressLint("IntentReset")
            @Override
            public void onClick(View v) {
                if (v.getId() == holder.smsCl.getId()) {

                    String phone = getItem(holder.position).getFirstPhoneNumber();
                    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", phone, null));
                    context.startActivity(i);
                }
                if (v.getId() == holder.callCl.getId()) {
                    callPhone(getItem(position).getFirstPhoneNumber());
                }
                if (v.getId() == holder.whatsappCl.getId()) {
                    ContactWithAllInformation contactWithAllInformation = getItem(position);
                    ContactGesture.INSTANCE.openWhatsapp(converter06To33(contactWithAllInformation.getFirstPhoneNumber()), context);
                }
                if (v.getId() == holder.mailCl.getId()) {
                    String mail = getItem(position).getFirstMail();
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setData(Uri.parse("mailto:"));
                    intent.setType("text/html");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mail.substring(0, mail.length() - 1)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "");
                    intent.putExtra(Intent.EXTRA_TEXT, "");
                    println("intent " + Objects.requireNonNull(intent.getExtras()).toString());
                    context.startActivity(Intent.createChooser(intent, "envoyer un mail à " + mail.substring(0, mail.length() - 1)));
                }
                if (v.getId() == holder.editCl.getId()) {
                    Intent intent = new Intent(context, EditContactActivity.class);
                    intent.putExtra("ContactId", contact.getId());
                    context.startActivity(intent);
                }
                if (holder.constraintLayoutMenu != null) {

                    if (holder.constraintLayoutMenu.getVisibility() == View.GONE) {
                        holder.constraintLayoutMenu.setVisibility(View.VISIBLE);
                    } else {
                        holder.constraintLayoutMenu.setVisibility(View.GONE);

                        Animation slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_down);
                        holder.constraintLayoutMenu.startAnimation(slideDown);
                    }
                }
            }
        };

        View.OnLongClickListener longClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                view.setTag(holder);
                ContactDB contact = gestionnaireContacts.getContacts().get(position).getContactDB();
                assert contact != null;

                holder.contactFirstNameView.setText(contact.getFirstName());

                if (listOfItemSelected.contains(gestionnaireContacts.getContacts().get(position))) {
                    listOfItemSelected.remove(gestionnaireContacts.getContacts().get(position));

                    if (!contact.getProfilePicture64().equals("")) {
                        Bitmap bitmap = base64ToBitmap(contact.getProfilePicture64());
                        holder.contactRoundedImageView.setImageBitmap(bitmap);
                    } else {
                        holder.contactRoundedImageView.setImageResource(randomDefaultImage(contact.getProfilePicture()));
                    }
                } else {
                    listOfItemSelected.add(gestionnaireContacts.getContacts().get(position));
                    holder.contactRoundedImageView.setImageResource(R.drawable.ic_contact_selected);
                }


                ((MainActivity) context).longRecyclerItemClick(position);
                return true;
            }
        };

        View.OnClickListener listItemClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).recyclerItemClick(len, position);
            }
        };


        if (!whatsappIsInstalled()) {
            holder.whatsappCl.setVisibility(View.INVISIBLE);
        } else {
            holder.whatsappCl.setVisibility(View.VISIBLE);
        }

        if (getItem(position).getFirstMail().isEmpty()) {
            holder.mailCl.setVisibility(View.INVISIBLE);
        } else {
            holder.mailCl.setVisibility(View.VISIBLE);
        }

        if (holder.constraintLayout != null) {
            holder.constraintLayout.setOnLongClickListener(longClick);
            holder.constraintLayout.setOnClickListener(listItemClick);
            holder.constraintLayout.setOnClickListener(listener);
        } else {
            holder.constraintLayoutSmaller.setOnLongClickListener(longClick);
            holder.constraintLayoutSmaller.setOnClickListener(listItemClick);
        }

        holder.editCl.setOnClickListener(listener);
        holder.mailCl.setOnClickListener(listener);
        holder.whatsappCl.setOnClickListener(listener);
        holder.callCl.setOnClickListener(listener);
        holder.smsCl.setOnClickListener(listener);
    }

    @Override
    public long getItemId(int position) {
        return listContacts.size();
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return listContacts.size();
    }

    private String converter06To33(String phoneNumber) {
        if (phoneNumber.charAt(0) == '0') {
            return "+33" + phoneNumber;
        }
        return phoneNumber;
    }

    private int randomDefaultImage(int avatarId) {
        switch (avatarId) {
            case 0:
                return R.drawable.ic_user_purple;
            case 1:
                return R.drawable.ic_user_blue;
            case 2:
                return R.drawable.ic_user_knocker;
            case 3:
                return R.drawable.ic_user_green;
            case 4:
                return R.drawable.ic_user_om;
            case 5:
                return R.drawable.ic_user_orange;
            case 6:
                return R.drawable.ic_user_pink;
            default:
                return R.drawable.ic_user_blue;
        }
    }

    private Bitmap base64ToBitmap(String base64) {
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inSampleSize = 2;
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, options);
    }

    public void callPhone(final String phoneNumber) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_CALL_RESULT);
            numberForPermission = phoneNumber;
        } else {
            //Intent intent=new Intent(Intent.ACTION_CALL);
            //intent.setData(Uri.parse(getItem(position).getFirstPhoneNumber()));
            SharedPreferences sharedPreferences = context.getSharedPreferences("Phone_call", Context.MODE_PRIVATE);
            boolean popup = sharedPreferences.getBoolean("popup", true);
            if (popup && numberForPermission.isEmpty()) {
                new AlertDialog.Builder(context)
                        .setTitle("Voulez-vous appeler ce contact ?")
                        .setMessage("Vous pouvez désactiver cette validation depuis les options")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                context.startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber, null)));
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            } else {
                context.startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber, null)));
                numberForPermission = "";
            }
        }
    }//code duplicate à mettre dans contactAllInfo

    private boolean whatsappIsInstalled() {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getApplicationInfo("com.whatsapp", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {
        public TextView contactFirstNameView;
        ConstraintLayout constraintLayout;
        ConstraintLayout constraintLayoutMenu;
        public CircularImageView contactRoundedImageView;
        ConstraintLayout constraintLayoutSmaller;
        ConstraintLayout constraintLayoutMenuSmaller;

        int position;

        ConstraintLayout callCl;
        ConstraintLayout smsCl;
        ConstraintLayout whatsappCl;
        ConstraintLayout mailCl;
        ConstraintLayout editCl;

        ConstraintLayout groupWordingConstraint;
        TextView groupWordingTv;

        ContactViewHolder(@NonNull View view) {
            super(view);

            if (len == 0) {
                contactRoundedImageView = view.findViewById(R.id.list_contact_item_contactRoundedImageView);
                contactFirstNameView = view.findViewById(R.id.list_contact_item_contactFirstName);
                constraintLayoutSmaller = view.findViewById(R.id.list_contact_item_layout_smaller);
                constraintLayoutMenuSmaller = view.findViewById(R.id.list_contact_item_menu_smaller);
                callCl = view.findViewById(R.id.list_contact_item_smaller_constraint_call);
                smsCl = view.findViewById(R.id.list_contact_item_smaller_constraint_sms);
                whatsappCl = view.findViewById(R.id.list_contact_item_smaller_constraint_whatsapp);
                mailCl = view.findViewById(R.id.list_contact_item_smaller_constraint_mail);
                editCl = view.findViewById(R.id.list_contact_item_smaller_constraint_edit);
                groupWordingConstraint = view.findViewById(R.id.list_contact_item_wording_group_constraint_layout);
                groupWordingTv = view.findViewById(R.id.list_contact_item_wording_group_tv);

            } else {
                contactRoundedImageView = view.findViewById(R.id.list_contact_item_contactRoundedImageView);
                contactFirstNameView = view.findViewById(R.id.list_contact_item_contactFirstName);
                constraintLayout = view.findViewById(R.id.list_contact_item_layout);
                constraintLayoutMenu = view.findViewById(R.id.list_contact_item_menu);
                callCl = view.findViewById(R.id.list_contact_item_constraint_call);
                smsCl = view.findViewById(R.id.list_contact_item_constraint_sms);
                whatsappCl = view.findViewById(R.id.list_contact_item_constraint_whatsapp);
                mailCl = view.findViewById(R.id.list_contact_item_constraint_mail);
                editCl = view.findViewById(R.id.list_contact_item_constraint_edit);
                groupWordingConstraint = view.findViewById(R.id.list_contact_wording_group_constraint_layout);
                groupWordingTv = view.findViewById(R.id.list_contact_wording_group_tv);
            }
        }
    }

    public String getPhonePermission() {
        return numberForPermission;
    }
}