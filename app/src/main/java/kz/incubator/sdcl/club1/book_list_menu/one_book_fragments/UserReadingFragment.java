package kz.incubator.sdcl.club1.book_list_menu.one_book_fragments;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import kz.incubator.sdcl.club1.R;
import kz.incubator.sdcl.club1.book_list_menu.OneBookAcvitiy;
import kz.incubator.sdcl.club1.book_list_menu.interfaces.ItemClickListener;
import kz.incubator.sdcl.club1.book_list_menu.interfaces.RecyclerItemClickListener;
import kz.incubator.sdcl.club1.database.StoreDatabase;
import kz.incubator.sdcl.club1.groups_menu.UserProfileActivity;
import kz.incubator.sdcl.club1.groups_menu.module.User;

public class UserReadingFragment extends Fragment {

    ReadingListAdapter readingListAdapter;
    RecyclerView recyclerView;
    private List<User> userList;
    View view;
    OneBookAcvitiy oneBookAcvitiy;
    DatabaseReference mDatabaseRef, userRef;
    StoreDatabase storeDb;
    SQLiteDatabase sqdb;
    ProgressBar progressBar;
    TextView checkIsEmpty;
    String userId, bookId, userGroupId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_user_book_list, container, false);

        if(getArguments() != null){
            bookId = getArguments().getString("bookId");
        }

        initialize();

        return view;
    }

    public void initialize() {
        recyclerView = view.findViewById(R.id.recyclerForBook);
        userList = new ArrayList<>();

        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            bookId = getArguments().getString("bookId");
        }

        oneBookAcvitiy = new OneBookAcvitiy();
        storeDb = new StoreDatabase(getActivity());
        sqdb = storeDb.getWritableDatabase();
        checkIsEmpty = view.findViewById(R.id.checkIsEmpty);
        progressBar = view.findViewById(R.id.ProgressBar);

        userRef = FirebaseDatabase.getInstance().getReference().child("user_list");
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("book_list").child(bookId).child("reading");
        userRef.child(userId).child("group_id").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userGroupId = dataSnapshot.getValue().toString();

                mDatabaseRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            userList.clear();
                            checkIsEmpty.setVisibility(View.GONE);

                            for (DataSnapshot users : dataSnapshot.getChildren()) {
                                String userId = users.getKey();

                                userRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        User user = dataSnapshot.getValue(User.class);

                                        if (user.getGroup_id().equals(userGroupId))
                                            userList.add(dataSnapshot.getValue(User.class));

                                        readingListAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }

                        } else {
                            checkIsEmpty.setVisibility(View.VISIBLE);
                        }

                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        readingListAdapter = new ReadingListAdapter(getActivity(), userList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(readingListAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, final int pos) {

                        Intent profile = new Intent(getActivity(), UserProfileActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("user",userList.get(pos));
                        profile.putExtras(bundle);
                        startActivity(profile);


                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }
                })
        );

    }

    public String checkAbone(String ticketDay) {
        String abone = "norm";

        DateFormat day = new SimpleDateFormat("dd");
        DateFormat month = new SimpleDateFormat("MM");
        DateFormat year = new SimpleDateFormat("yyyy");
        int first_slash = ticketDay.indexOf('/');
        int last_slash = ticketDay.lastIndexOf('/');

        String period_day = ticketDay.substring(0, first_slash);
        String period_month = ticketDay.substring(first_slash + 1, last_slash);
        String period_year = ticketDay.substring(last_slash + 1, ticketDay.length());

        Calendar cal = Calendar.getInstance();

        if (period_year.equals(year.format(cal.getTime()))) {
            if (period_month.equals(month.format(cal.getTime()))) {
                if (period_day.equals(day.format(cal.getTime()))) {
                    abone = "last day";
                } else if (Integer.parseInt(period_day) > Integer.parseInt(day.format(cal.getTime()))) {
                    int counter = Integer.parseInt(period_day) - Integer.parseInt(day.format(cal.getTime()));
                    if (counter <= 7) {
                        abone = counter + " days left";
                    }
                } else {
                    abone = "Your subscription is up";

                }
            } else if (Integer.parseInt(period_month) < Integer.parseInt(month.format(cal.getTime()))) {
                abone = "Your subscription is up";

            }
        } else if (Integer.parseInt(period_year) < Integer.parseInt(year.format(cal.getTime()))) {
            abone = "Your subscription is up";

        }

        return abone;
    }

    public class ReadingListAdapter extends RecyclerView.Adapter<ReadingListAdapter.MyTViewHolder>{
        private Context context;
        private List<User> userList;
        ItemClickListener itemClickListener;

        public class MyTViewHolder extends RecyclerView.ViewHolder{
            public TextView info, phone_number, userGroup;
            public ImageView person_photo;

            public MyTViewHolder(View view) {
                super(view);
                person_photo = view.findViewById(R.id.person_photo);
                info = view.findViewById(R.id.info);
                userGroup = view.findViewById(R.id.userGroup);
                phone_number = view.findViewById(R.id.number);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        itemClickListener.onItemClick(view,getPosition());
                    }
                });
            }

        }


        public ReadingListAdapter(Context context, List<User> userList) {
            this.context = context;
            this.userList = userList;
        }

        public void setOnClickListener(ItemClickListener itemClickListener){
            this.itemClickListener = itemClickListener;
        }

        @Override
        public MyTViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_readed, parent, false);
            return new MyTViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyTViewHolder holder, int position) {
            User item = userList.get(position);

            Glide.with(context)
                    .load(item.getPhoto())
                    .placeholder(R.drawable.user_def)
                    .into(holder.person_photo);

            holder.info.setText(item.getInfo());
            holder.userGroup.setText("Group: "+item.getGroupName());
            holder.phone_number.setText(item.getPhoneNumber().toString());

        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

    }
}


