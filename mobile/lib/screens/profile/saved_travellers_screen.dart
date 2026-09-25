import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/theme.dart';
import '../../models/user_model.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/custom_button.dart';
import '../../widgets/custom_text_field.dart';

class SavedTravellersScreen extends StatelessWidget {
  const SavedTravellersScreen({super.key});

  void _showAddTravellerDialog(BuildContext context, AuthProvider authProvider) {
    final nameController = TextEditingController();
    final ageController = TextEditingController();
    String gender = 'Male';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) {
          final isDark = Theme.of(context).brightness == Brightness.dark;
          return Container(
            padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(context).viewInsets.bottom + 20),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkSurface : Colors.white,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Add New Traveller', style: TextStyle(fontFamily: 'Outfit', fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                CustomTextField(
                  label: 'Full Name',
                  hintText: 'Enter name',
                  controller: nameController,
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      flex: 1,
                      child: CustomTextField(
                        label: 'Age',
                        hintText: 'Age',
                        controller: ageController,
                        keyboardType: TextInputType.number,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      flex: 2,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Gender', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 6),
                          Row(
                            children: ['Male', 'Female', 'Other'].map((g) {
                              final isSel = gender == g;
                              return Expanded(
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(horizontal: 2),
                                  child: InkWell(
                                    onTap: () => setModalState(() => gender = g),
                                    child: Container(
                                      padding: const EdgeInsets.symmetric(vertical: 12),
                                      decoration: BoxDecoration(
                                        color: isSel ? AppColors.primary : const Color(0xFFF3F4F6),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Center(
                                        child: Text(
                                          g,
                                          style: TextStyle(
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                            color: isSel ? Colors.white : AppColors.textPrimary,
                                          ),
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              );
                            }).toList(),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                CustomButton(
                  text: 'SAVE TRAVELLER',
                  onPressed: () {
                    if (nameController.text.trim().isNotEmpty) {
                      authProvider.addSavedTraveller(
                        SavedTraveller(
                          id: 'st_${DateTime.now().millisecondsSinceEpoch}',
                          name: nameController.text.trim(),
                          age: int.tryParse(ageController.text) ?? 25,
                          gender: gender,
                        ),
                      );
                      Navigator.pop(ctx);
                    }
                  },
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final travellers = authProvider.user?.savedTravellers ?? [];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Saved Co-Travellers'),
      ),
      body: travellers.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.people_outline_rounded, size: 54, color: AppColors.textMuted),
                  const SizedBox(height: 12),
                  const Text('No saved travellers yet', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 6),
                  const Text('Add frequent co-passengers for faster checkout', style: TextStyle(color: AppColors.textSecondary)),
                  const SizedBox(height: 20),
                  ElevatedButton.icon(
                    onPressed: () => _showAddTravellerDialog(context, authProvider),
                    icon: const Icon(Icons.add),
                    label: const Text('Add Traveller'),
                  ),
                ],
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: travellers.length,
              itemBuilder: (context, index) {
                final t = travellers[index];
                return Container(
                  margin: const EdgeInsets.only(bottom: 10),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: isDark ? AppColors.darkCard : Colors.white,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: isDark ? const Color(0xFF374151) : AppColors.border),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.primaryLight,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          t.gender == 'Female' ? Icons.woman_rounded : Icons.man_rounded,
                          color: AppColors.primary,
                          size: 22,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(t.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                            Text('${t.age} yrs • ${t.gender}', style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
                          ],
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.delete_outline_rounded, color: Colors.redAccent, size: 20),
                        onPressed: () => authProvider.removeSavedTraveller(t.id),
                      ),
                    ],
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        onPressed: () => _showAddTravellerDialog(context, authProvider),
        icon: const Icon(Icons.add),
        label: const Text('Add New'),
      ),
    );
  }
}
