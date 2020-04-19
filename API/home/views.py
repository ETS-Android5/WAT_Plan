from django.shortcuts import render, redirect
from django.utils.encoding import smart_str
from django.http import HttpResponse

from .models import Apk


def home(request, *args, **kwargs):
    apks = Apk.objects.all().order_by('-release_date')
    latest_version = apks.first().version
    context = {
        'apks': apks,
        'latest_version': latest_version,
    }
    return render(request, 'home/home.html', context)


def download_apk(request, *args, **kwargs):
    # path_to_file = get_path_to_course_download(course)
    file_name = 'WAT_Plan_1.0.apk'
    response = HttpResponse(content_type='application/force-download')
    response['Content-Disposition'] = f'attachment; filename={smart_str(file_name)}'
    response['X-Sendfile'] = smart_str('/media/apk/'+file_name)
    return response